package com.grafka.entities.topics

import graphql.PublicApi

@PublicApi
data class KafkaReplicationAssignment(val partition: Int, val assignments: List<Int>)