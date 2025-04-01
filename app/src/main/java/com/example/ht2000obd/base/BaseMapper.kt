package com.example.ht2000obd.base

/**
 * Base Mapper interface for mapping between domain models and data models
 *
 * @param I Input type
 * @param O Output type
 */
interface BaseMapper<I, O> {
    fun map(input: I): O
}

/**
 * Base Mapper interface for bi-directional mapping
 *
 * @param M Model type (e.g., domain model)
 * @param E Entity type (e.g., database entity)
 */
interface BaseBiDirectionalMapper<M, E> {
    fun mapToEntity(model: M): E
    fun mapFromEntity(entity: E): M
}

/**
 * Base List Mapper interface for mapping lists of models
 *
 * @param I Input type
 * @param O Output type
 */
interface BaseListMapper<I, O> : BaseMapper<List<I>, List<O>>

/**
 * Base Nullable Mapper interface for mapping nullable types
 *
 * @param I Input type
 * @param O Output type
 */
interface BaseNullableMapper<I, O> {
    fun map(input: I?): O?
}

/**
 * Base Bi-Directional List Mapper interface
 *
 * @param M Model type
 * @param E Entity type
 */
interface BaseBiDirectionalListMapper<M, E> {
    fun mapToEntityList(models: List<M>): List<E>
    fun mapFromEntityList(entities: List<E>): List<M>
}

/**
 * Extension function to map lists using a mapper
 */
fun <I, O> BaseMapper<I, O>.mapList(input: List<I>): List<O> {
    return input.map(::map)
}

/**
 * Extension function to map nullable types using a mapper
 */
fun <I, O> BaseMapper<I, O>.mapNullable(input: I?): O? {
    return input?.let(::map)
}

/**
 * Extension function to map lists using a bi-directional mapper (to entity)
 */
fun <M, E> BaseBiDirectionalMapper<M, E>.mapToEntityList(models: List<M>): List<E> {
    return models.map(::mapToEntity)
}

/**
 * Extension function to map lists using a bi-directional mapper (from entity)
 */
fun <M, E> BaseBiDirectionalMapper<M, E>.mapFromEntityList(entities: List<E>): List<M> {
    return entities.map(::mapFromEntity)
}

/**
 * Abstract base class for mappers with common functionality
 */
abstract class Mapper<I, O> : BaseMapper<I, O> {
    
    /**
     * Map a list of inputs to outputs
     */
    fun mapList(input: List<I>): List<O> {
        return input.map(::map)
    }

    /**
     * Map a nullable input to nullable output
     */
    fun mapNullable(input: I?): O? {
        return input?.let(::map)
    }
}

/**
 * Abstract base class for bi-directional mappers
 */
abstract class BiDirectionalMapper<M, E> : BaseBiDirectionalMapper<M, E> {
    
    /**
     * Map a list of models to entities
     */
    fun mapToEntityList(models: List<M>): List<E> {
        return models.map(::mapToEntity)
    }

    /**
     * Map a list of entities to models
     */
    fun mapFromEntityList(entities: List<E>): List<M> {
        return entities.map(::mapFromEntity)
    }

    /**
     * Map a nullable model to nullable entity
     */
    fun mapToEntityNullable(model: M?): E? {
        return model?.let(::mapToEntity)
    }

    /**
     * Map a nullable entity to nullable model
     */
    fun mapFromEntityNullable(entity: E?): M? {
        return entity?.let(::mapFromEntity)
    }
}

/**
 * Abstract base class for list mappers
 */
abstract class ListMapper<I, O> : BaseListMapper<I, O> {
    override fun map(input: List<I>): List<O> {
        return input.map { mapItem(it) }
    }

    /**
     * Map a single item
     */
    protected abstract fun mapItem(input: I): O
}

/**
 * Abstract base class for nullable mappers
 */
abstract class NullableMapper<I, O> : BaseNullableMapper<I, O> {
    override fun map(input: I?): O? {
        return input?.let { mapNonNull(it) }
    }

    /**
     * Map a non-null item
     */
    protected abstract fun mapNonNull(input: I): O
}