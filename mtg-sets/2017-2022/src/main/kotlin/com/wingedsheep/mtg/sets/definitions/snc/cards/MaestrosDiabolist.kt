package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Maestros Diabolist
 * {U}{B}{R}
 * Creature — Vampire Warrior
 * 1/4
 * Deathtouch, haste
 * Whenever this creature attacks, if you don't control a Devil token, create a tapped and attacking
 * 1/1 red Devil creature token with "When this token dies, it deals 1 damage to any target."
 *
 * The Devil-token check is an intervening "if" (CR 603.4): it's tested when the trigger would
 * trigger and again on resolution, so a Devil token created in response stops the second one.
 */
val MaestrosDiabolist = card("Maestros Diabolist") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Creature — Vampire Warrior"
    oracleText = "Deathtouch, haste\n" +
        "Whenever this creature attacks, if you don't control a Devil token, create a tapped and " +
        "attacking 1/1 red Devil creature token with \"When this token dies, it deals 1 damage to any target.\""
    power = 1
    toughness = 4

    keywords(Keyword.DEATHTOUCH, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Permanent.withSubtype("Devil").token(),
            negate = true,
        )
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
            imageUri = "https://cards.scryfall.io/normal/front/e/c/ec75fdab-818c-4f3a-94c1-fd3f577a87f5.jpg?1783922960",
        )
        description = "Whenever this creature attacks, if you don't control a Devil token, create a " +
            "tapped and attacking 1/1 red Devil creature token with \"When this token dies, it deals " +
            "1 damage to any target.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Bastien L. Deharme"
        flavorText = "\"Where did I find my horned friend? Now there's a story worth telling. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7206db10-e248-420d-bf92-4d57bc72da56.jpg?1783923079"
    }
}
