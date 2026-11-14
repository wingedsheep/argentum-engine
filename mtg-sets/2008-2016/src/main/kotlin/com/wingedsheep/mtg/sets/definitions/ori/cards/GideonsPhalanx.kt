package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gideon's Phalanx
 * {5}{W}{W}
 * Instant
 *
 * Create four 2/2 white Knight creature tokens with vigilance.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * creatures you control gain indestructible until end of turn.
 *
 * Follows current Oracle wording: the tokens are plain Knights (no Ally subtype).
 *
 * The tokens are created first, so the spell-mastery group grant (evaluated at resolution, after
 * the tokens exist) covers them too, per the 2015-06-22 ruling.
 */
val GideonsPhalanx = card("Gideon's Phalanx") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Create four 2/2 white Knight creature tokens with vigilance.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "creatures you control gain indestructible until end of turn."

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            count = 4,
            imageUri = "https://cards.scryfall.io/normal/front/1/b/1bc2969b-2176-4471-b316-9c80443866dd.jpg?1783938295"
        ) then Effects.If(
            condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
            then = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30142729-de9a-4a0b-b060-8f68e1ef234d.jpg?1783938363"
        ruling("2015-06-22", "If the spell mastery ability applies, the four Knight tokens will also gain indestructible until end of turn.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
