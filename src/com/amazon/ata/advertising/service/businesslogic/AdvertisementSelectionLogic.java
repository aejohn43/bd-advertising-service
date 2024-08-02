package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.CustomerProfileDao;
import com.amazon.ata.advertising.service.dao.CustomerSpendDao;
import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.AdvertisementContent;
import com.amazon.ata.advertising.service.model.EmptyGeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.GeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    private TargetingEvaluator targetingEvaluator;

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao, TargetingEvaluator targetingEvaluator) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;
        this.targetingEvaluator = targetingEvaluator;
    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */
    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {
        targetingEvaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));
        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();
        if (StringUtils.isEmpty(marketplaceId) ) {
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
        }
        else {
            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);

            /*if (CollectionUtils.isNotEmpty(contents)) {
                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
            }*/
            //.filter(content -> targetingEvaluator.evaluate(targetingGroupDao.get(content.getContentId()).get(0)).isTrue())
            //this for letter coming from bellow
                Optional<TargetingGroup> subContents = Optional.of(contents)
                        .orElse(Collections.emptyList())
                        .stream()
                        .flatMap(content -> {
                            return targetingGroupDao.get(content.getContentId()).stream();
                        })
                        .sorted(Comparator.comparing(TargetingGroup::getClickThroughRate))
                        .filter(targetingGroup -> targetingEvaluator.evaluate(targetingGroup).isTrue() )
                        //.filter(content -> targetingEvaluator.evaluate(targetingGroupDao.get(content.getContentId()).get(0)).isTrue())
                        .findFirst();
                if (subContents.isPresent()){
                    return new GeneratedAdvertisement(Optional.of(contents).orElse(Collections.emptyList())
                            .stream()
                            .filter(content -> Objects.equals(content.getContentId(), subContents.get().getContentId()))
                            .findFirst()
                            .get());
                }
        }
        return generatedAdvertisement;
    }
}
