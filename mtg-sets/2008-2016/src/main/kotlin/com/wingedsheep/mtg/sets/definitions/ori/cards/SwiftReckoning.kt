package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Swift Reckoning
 * {1}{W}
 * Sorcery
 *
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you may
 * cast this spell as though it had flash.
 * Destroy target tapped creature.
 *
 * Spell mastery here is a cast-time [conditionalFlash] gate. The count excludes Swift Reckoning
 * itself (`Not(IsSource)`): cast from a graveyard, it needs two *other* instant and/or sorcery cards
 * there (2018-12-07 ruling); from hand the exclusion is a no-op.
 */
val SwiftReckoning = card("Swift Reckoning") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "you may cast this spell as though it had flash. (You may cast it any time you could cast an instant.)\n" +
        "Destroy target tapped creature."

    conditionalFlash = Conditions.CardsInGraveyardMatchingAtLeast(
        2,
        GameObjectFilter.InstantOrSorcery.withStatePredicate(StatePredicate.Not(StatePredicate.IsSource))
    )

    spell {
        val t = target(TargetFilter.Creature.tapped())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/904cb2f5-eb62-4416-8236-d2fbeadf1dc4.jpg?1783938357"
        ruling("2018-12-07", "The number of instant and/or sorcery cards in your graveyard matters only as you begin to cast Swift Reckoning. Once it's cast, that number doesn't matter and will have no effect on Swift Reckoning resolving.")
        ruling("2018-12-07", "If an effect allows you to cast Swift Reckoning from your graveyard, you need two other instant and/or sorcery cards in your graveyard to cast it as though it had flash.")
    }
}
