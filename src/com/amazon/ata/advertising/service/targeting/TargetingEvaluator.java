package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;
    ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    @Inject
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();
        /*boolean allTruePredicates = true;
        for (TargetingPredicate predicate : targetingPredicates) {
            TargetingPredicateResult predicateResult = predicate.evaluate(requestContext);
            if (!predicateResult.isTrue()) {
                allTruePredicates = false;
                break;
            }
        }*/
        Optional<TargetingPredicate> optional = targetingPredicates.stream()
                .filter(predicate -> {
                    Future<Boolean> future = executorService.submit(() -> {
                        return !predicate.evaluate(requestContext).isTrue();
                    });
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findAny();
        if (optional.isPresent()){
            return TargetingPredicateResult.FALSE;
        }
        else {
            return TargetingPredicateResult.TRUE;
        }
        /*return allTruePredicates ? TargetingPredicateResult.TRUE :
                                   TargetingPredicateResult.FALSE;*/
    }
}
