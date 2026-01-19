package com.wingedsheep.rulesengine.sets

import com.wingedsheep.rulesengine.ability.CardScript
import com.wingedsheep.rulesengine.card.CardDefinition

/**
 * Interface for card set registries.
 * Each set (Portal, Alpha, etc.) implements this interface to register its cards.
 */
interface CardRegistry {
    /** The set code (e.g., "POR" for Portal) */
    val setCode: String

    /** The set name (e.g., "Portal") */
    val setName: String

    /**
     * Get all card definitions in this set.
     */
    fun getCardDefinitions(): List<CardDefinition>

    /**
     * Get all card scripts in this set.
     */
    fun getCardScripts(): List<CardScript>

    /**
     * Get a card definition by name.
     */
    fun getCardDefinition(name: String): CardDefinition?

    /**
     * Get a card script by name.
     */
    fun getCardScript(name: String): CardScript?

    /**
     * Get the number of cards in this set.
     */
    val cardCount: Int
        get() = getCardDefinitions().size
}

/**
 * Base implementation of CardRegistry that stores cards in maps.
 */
abstract class BaseCardRegistry : CardRegistry {
    protected val definitions = mutableMapOf<String, CardDefinition>()
    protected val scripts = mutableMapOf<String, CardScript>()

    override fun getCardDefinitions(): List<CardDefinition> = definitions.values.toList()

    override fun getCardScripts(): List<CardScript> = scripts.values.toList()

    override fun getCardDefinition(name: String): CardDefinition? = definitions[name]

    override fun getCardScript(name: String): CardScript? = scripts[name]

    /**
     * Register a card with its definition and script.
     */
    protected fun register(definition: CardDefinition, script: CardScript) {
        definitions[definition.name] = definition
        scripts[script.cardName] = script
    }

    /**
     * Register a vanilla card (no abilities).
     */
    protected fun registerVanilla(definition: CardDefinition) {
        definitions[definition.name] = definition
        scripts[definition.name] = CardScript.empty(definition.name)
    }

    /**
     * Register a French vanilla card (keywords only, from the definition).
     */
    protected fun registerFrenchVanilla(definition: CardDefinition) {
        definitions[definition.name] = definition
        scripts[definition.name] = CardScript.withKeywords(definition.name, *definition.keywords.toTypedArray())
    }

    /**
     * Register just a script without a definition.
     * Used for back faces of double-faced cards that share a definition.
     */
    protected fun registerScript(script: CardScript) {
        scripts[script.cardName] = script
    }
}
