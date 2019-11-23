package com.grafka.resolvers

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.grafka.configs.AdminClientFactory
import com.grafka.entities.topics.KafkaNewTopicRequest
import com.grafka.entities.topics.KafkaTopicListing
import org.springframework.stereotype.Component

@Component
class KafkaTopicMutationResolver(private val adminClientFactory: AdminClientFactory, private val adminResolver: KafkaAdminResolver) : GraphQLMutationResolver {

    fun newTopic(clusterId: String, request: KafkaNewTopicRequest): KafkaTopicListing {
        val topic = request.toNewTopic()

        adminClientFactory
                .getAdminClient(clusterId)
                .createTopics(listOf(topic))
                .all()
                .get()

        // TODO how to prevent partials?
        return adminResolver.topicListings(clusterId, topic.name()).first()
    }

}