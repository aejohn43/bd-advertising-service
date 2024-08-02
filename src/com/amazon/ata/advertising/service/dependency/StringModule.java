package com.amazon.ata.advertising.service.dependency;

import dagger.Module;
import dagger.Provides;

@Module
public class StringModule {
    @Provides
    public String provideString () {
        return "";
    }
}
