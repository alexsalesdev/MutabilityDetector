package org.mutabilitydetector.benchmarks.sealed;

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

public final class IsFinalAndHasOnlyPrivateConstructors {
    private final long something;
    
    private IsFinalAndHasOnlyPrivateConstructors(int something) {
        this.something = something;
       
    }
    
    private IsFinalAndHasOnlyPrivateConstructors() {
        this(43);
        
    }

    public long getSomething() {
        return something;
    }
    
    public static IsFinalAndHasOnlyPrivateConstructors newUp(int something) {
        return new IsFinalAndHasOnlyPrivateConstructors(something);
    }
}
