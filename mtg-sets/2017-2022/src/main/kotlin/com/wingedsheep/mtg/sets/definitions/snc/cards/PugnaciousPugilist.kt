package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Pugnacious Pugilist
 * {3}{R}{R}
 * Creature — Ogre Warrior
 * 4/4
 * Whenever this creature attacks, create a tapped and attacking 1/1 red Devil creature token with
 * "When this token dies, it deals 1 damage to any target."
 * Blitz {3}{R} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val PugnaciousPugilist = card("Pugnacious Pugilist") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "Whenever this creature attacks, create a tapped and attacking 1/1 red Devil creature " +
        "token with \"When this token dies, it deals 1 damage to any target.\"\n" +
        "Blitz {3}{R} (If you cast this spell for its blitz cost, it gains haste and \"When this " +
        "creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
            tapped = true,
            attacking = true,
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    val any = target(Targets.Any)
                    effect = Effects.DealDamage(1, any)
                    description = "When this token dies, it deals 1 damage to any target."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/e/c/ec75fdab-818c-4f3a-94c1-fd3f577a87f5.jpg?1783922960"
        )
    }

    keywordAbility(KeywordAbility.blitz("{3}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d98022d2-3b46-41f4-8f31-0a238d881ac6.jpg?1783923117"
    }
}
