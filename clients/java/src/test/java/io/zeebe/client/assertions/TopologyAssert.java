/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.assertions;

import io.zeebe.client.api.response.BrokerInfo;
import io.zeebe.client.api.response.Topology;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;

public class TopologyAssert extends AbstractAssert<TopologyAssert, Topology> {

  public TopologyAssert(final Topology topology) {
    super(topology, TopologyAssert.class);
  }

  public static TopologyAssert assertThat(Topology actual) {
    return new TopologyAssert(actual);
  }

  public final TopologyAssert isComplete(final int clusterSize, final int partitionCount) {
    isNotNull();

    final List<BrokerInfo> brokers = actual.getBrokers();

    if (brokers.size() != clusterSize) {
      failWithMessage("Expected broker count to be <%s> but was <%s>", clusterSize, brokers.size());
    }

    final List<BrokerInfo> brokersWithUnexpectedPartitionCount =
        brokers.stream()
            .filter(b -> b.getPartitions().size() != partitionCount)
            .collect(Collectors.toList());

    if (!brokersWithUnexpectedPartitionCount.isEmpty()) {
      failWithMessage(
          "Expected <%s> partitions at each broker, but found brokers with different partition count <%s>",
          partitionCount, brokersWithUnexpectedPartitionCount);
    }

    return this;
  }
}
