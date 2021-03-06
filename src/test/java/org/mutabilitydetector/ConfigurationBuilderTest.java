package org.mutabilitydetector;

/*
 * #%L
 * MutabilityDetector
 * %%
 * Copyright (C) 2008 - 2014 Graham Allan
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.junit.Test;
import org.mutabilitydetector.checkers.CheckerRunner.ExceptionPolicy;
import org.mutabilitydetector.checkers.MutabilityAnalysisException;
import org.mutabilitydetector.checkers.MutabilityCheckerFactory.ReassignedFieldAnalysisChoice;
import org.mutabilitydetector.checkers.info.CopyMethod;
import org.mutabilitydetector.config.HardcodedResultsUsage;
import org.mutabilitydetector.locations.Dotted;
import org.mutabilitydetector.unittesting.internal.CloneList;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mutabilitydetector.AnalysisResult.analysisResult;
import static org.mutabilitydetector.AnalysisResult.definitelyImmutable;
import static org.mutabilitydetector.checkers.CheckerRunner.ExceptionPolicy.CARRY_ON;
import static org.mutabilitydetector.checkers.CheckerRunner.ExceptionPolicy.FAIL_FAST;
import static org.mutabilitydetector.checkers.MutabilityCheckerFactory.ReassignedFieldAnalysisChoice.NAIVE_PUT_FIELD_ANALYSIS;
import static org.mutabilitydetector.config.HardcodedResultsUsage.DIRECTLY_IN_ASSERTION;
import static org.mutabilitydetector.config.HardcodedResultsUsage.LOOKUP_WHEN_REFERENCED;
import static org.mutabilitydetector.locations.Dotted.dotted;
import static org.mutabilitydetector.unittesting.AllowedReason.provided;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

public class ConfigurationBuilderTest {

    @Test
    public void canMergeResultsFromExistingConfiguration() throws Exception {
        final Configuration existing = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeResult(definitelyImmutable("hardcoded.in.other.Configuration"));
            }
        }.build();
        
        final Configuration current = new ConfigurationBuilder() {
            @Override public void configure() {
                mergeHardcodedResultsFrom(existing);
            }
        }.build();
        
        assertThat(current.hardcodedResults(), hasKey(dotted("hardcoded.in.other.Configuration")));
    }

    @Test
    public void mergeReplacesExistingHardcodedResultForClassWithCurrentHardcodedResult() throws Exception {
        final Configuration existing = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeResult(definitelyImmutable("hardcoded.in.both.Configurations"));
                hardcodeResult(definitelyImmutable("only.in.existing.Configuration"));
            }
        }.build();
        
        final AnalysisResult resultInCurrentConfig = analysisResult("hardcoded.in.both.Configurations",
                IsImmutable.NOT_IMMUTABLE,
                TestUtil.unusedMutableReasonDetail());
        
        final Configuration current = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeResult(resultInCurrentConfig);
                hardcodeResult(definitelyImmutable("only.in.current.Configuration"));

                mergeHardcodedResultsFrom(existing);
            }
        }.build();
        
        Map<Dotted, AnalysisResult> hardcodedResults = current.hardcodedResults();
        
        assertThat(hardcodedResults.size(), is(3));
        assertThat(hardcodedResults, hasEntry(dotted("hardcoded.in.both.Configurations"), resultInCurrentConfig));
        assertThat(hardcodedResults, hasEntry(dotted("only.in.existing.Configuration"), definitelyImmutable("only.in.existing.Configuration")));
        assertThat(hardcodedResults, hasEntry(dotted("only.in.current.Configuration"), definitelyImmutable("only.in.current.Configuration")));
    }
    
    @Test
    public void builtConfigurationsAreImmutable() throws Exception {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder() {
            @Override public void configure() { }
        };

        assertInstancesOf(configurationBuilder.build().getClass(),
                areImmutable(),
                provided(AnalysisResult.class, Dotted.class).areAlsoImmutable());
    }

    @Test
    public void canMergeEntireConfigurations() throws Exception {
        final Configuration first = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeResult(definitelyImmutable("hardcoded.in.both.Configurations"));
                hardcodeResult(definitelyImmutable("only.in.existing.Configuration"));
                useAdvancedReassignedFieldAlgorithm();
                setHowToUseHardcodedResults(DIRECTLY_IN_ASSERTION);
            }
        }.build();

        final Configuration second = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeAsDefinitelyImmutable("only.in.second.Configuration");
                hardcodeAsImmutableContainerType("container.only.in.second.Configuration");
                hardcodeValidCopyMethod(List.class, "com.google.common.collect.Lists.newArrayList", Iterable.class);
                setExceptionPolicy(FAIL_FAST);
                setHowToUseHardcodedResults(LOOKUP_WHEN_REFERENCED);
            }
        }.build();

        final AnalysisResult resultInCurrentConfig = analysisResult("hardcoded.in.both.Configurations",
                IsImmutable.NOT_IMMUTABLE,
                TestUtil.unusedMutableReasonDetail());

        final Configuration current = new ConfigurationBuilder() {
            @Override public void configure() {
                setExceptionPolicy(CARRY_ON);
                setHowToUseHardcodedResults(DIRECTLY_IN_ASSERTION);
                hardcodeResult(resultInCurrentConfig);
                merge(first);
                merge(second);
            }
        }.build();

        assertThat(current.hardcodedResults().size(), is(3));
        assertThat(current.immutableContainerClasses(), contains(Dotted.dotted("container.only.in.second.Configuration")));
        assertThat(current.hardcodedCopyMethods().get("java.util.List"),
            contains(new CopyMethod(dotted("com.google.common.collect.Lists"), "newArrayList", "(Ljava/lang/Iterable;)Ljava/util/ArrayList;")));
        assertThat(current.exceptionPolicy(), is(CARRY_ON));
        assertThat(current.howToUseHardcodedResults(), is(DIRECTLY_IN_ASSERTION));
        assertThat(current.reassignedFieldAlgorithm(), is(NAIVE_PUT_FIELD_ANALYSIS));
    }
    
    @Test
    public void mergeCopyMethodsDoesNotCauseDuplicates() {
        final CopyMethod a = new CopyMethod(dotted("any"), "method", "A");
        final CopyMethod b = new CopyMethod(dotted("any"), "method", "B");
        final CopyMethod c = new CopyMethod(dotted("any"), "method", "C");
        final CopyMethod a2 = new CopyMethod(dotted("any"), "method", "A");

        final Configuration original = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeValidCopyMethod(List.class, a);
                hardcodeValidCopyMethod(List.class, b);
            }
        }.build();

        final Configuration merged = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeValidCopyMethod(List.class, c);
                hardcodeValidCopyMethod(List.class, a2);
                mergeValidCopyMethodsFrom(original);
            }

        }.build();
        
        assertThat(merged.hardcodedCopyMethods().values(), containsInAnyOrder(a, b, c));
    }
    
    @Test (expected=MutabilityAnalysisException.class)
    public void shouldThrowIfClassOfCopyMethodIsNotKnown() {
        new ConfigurationBuilder() { 
            @Override public void configure() {
                hardcodeValidCopyMethod(List.class, "non.existent.Collection.<init>", List.class);
            }
        }.build();
    }

    @Test (expected=MutabilityAnalysisException.class)
    public void shouldThrowIfCopyMethodDoesNotExist() {
        new ConfigurationBuilder() { 
            @Override public void configure() {
                hardcodeValidCopyMethod(List.class, "com.google.common.collect.Lists.doesNotExist", List.class);
            }
        }.build();
    }
    
    @Test
    public void constructorsCanBeValidCopyMethods() {
        final Configuration cfg = new ConfigurationBuilder() {
            @Override public void configure() {
                hardcodeValidCopyMethod(CloneList.class, 
                        "org.mutabilitydetector.unittesting.internal.CloneList.<init>", List.class);
            }
        }.build();

        assertThat(cfg.hardcodedCopyMethods().size(), is(1));
    }
}
