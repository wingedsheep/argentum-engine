package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tezzeret the Schemer — Aether Revolt #137
 * {2}{U}{B} · Legendary Planeswalker — Tezzeret · Starting loyalty 5
 *
 * +1: Create a colorless artifact token named Etherium Cell with "{T}, Sacrifice this token: Add
 *     one mana of any color."
 * −2: Target creature gets +X/-X until end of turn, where X is the number of artifacts you control.
 * −7: You get an emblem with "At the beginning of combat on your turn, target artifact you control
 *     becomes an artifact creature with base power and toughness 5/5."
 *
 * Modeling notes:
 *
 *  - **Etherium Cell** is a predefined token (`PredefinedTokens.EtheriumCell`): a subtype-less
 *    artifact named Etherium Cell with the Treasure-style mana ability. Its art is TAER's, synced
 *    into `tokens.json` under AER.
 *  - **The −2's X** is counted once, as the ability resolves ([Effects.ModifyStats] evaluates its
 *    amounts at resolution), so it doesn't change later in the turn (2017-02-09 ruling).
 *  - **The emblem** is a permanent global triggered ability with the emblem as its colorless
 *    source. Its animation is [Duration.Permanent] ("lasts indefinitely") and sets *base* P/T
 *    5/5 in layer 7b, so an artifact creature or a crewed Vehicle ends up 5/5 too (rulings).
 */
val TezzeretTheSchemer = card("Tezzeret the Schemer") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 5
    oracleText = "+1: Create a colorless artifact token named Etherium Cell with \"{T}, Sacrifice " +
        "this token: Add one mana of any color.\"\n" +
        "−2: Target creature gets +X/-X until end of turn, where X is the number of artifacts you " +
        "control.\n" +
        "−7: You get an emblem with \"At the beginning of combat on your turn, target artifact you " +
        "control becomes an artifact creature with base power and toughness 5/5.\""

    // +1: Create an Etherium Cell.
    loyaltyAbility(+1) {
        effect = Effects.CreatePredefinedToken("Etherium Cell")
        description = "Create a colorless artifact token named Etherium Cell with \"{T}, Sacrifice " +
            "this token: Add one mana of any color.\""
    }

    // −2: Target creature gets +X/-X until end of turn, where X is the number of artifacts you control.
    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        val artifacts = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
        effect = Effects.ModifyStats(artifacts, -artifacts, creature)
        description = "Target creature gets +X/-X until end of turn, where X is the number of " +
            "artifacts you control."
    }

    // −7: Emblem — "At the beginning of combat on your turn, target artifact you control becomes
    //     an artifact creature with base power and toughness 5/5."
    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
                val artifact = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
                effect = Effects.BecomeCreature(
                    target = artifact,
                    power = 5,
                    toughness = 5,
                    duration = Duration.Permanent
                )
                description = "At the beginning of combat on your turn, target artifact you " +
                    "control becomes an artifact creature with base power and toughness 5/5."
            },
            emblem = true,
            descriptionOverride = "At the beginning of combat on your turn, target artifact you " +
                "control becomes an artifact creature with base power and toughness 5/5."
        )
        description = "You get an emblem with \"At the beginning of combat on your turn, target " +
            "artifact you control becomes an artifact creature with base power and toughness 5/5.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "137"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58265203-bc16-41d2-875c-2ff3b4870824.jpg?1783936735"

        ruling("2017-02-09", "The value of X for Tezzeret's second ability is determined only as the ability resolves. It won't change later in the turn if the number of artifacts you control changes.")
        ruling("2017-02-09", "The effect of the emblem's triggered ability lasts indefinitely.")
        ruling("2017-02-09", "An artifact that becomes a creature due to the emblem's ability can't attack unless you've controlled it continuously since your turn began. It doesn't matter whether or not it was a creature at that time.")
        ruling("2017-02-09", "An artifact creature targeted by the emblem's ability becomes 5/5 instead of its normal power and toughness.")
        ruling("2017-02-09", "A Vehicle targeted by the emblem's ability becomes a 5/5 creature. Crewing it won't change its power and toughness.")
        ruling("2017-02-09", "If an Equipment becomes a creature, it becomes unattached and it can't be attached to a creature.")
    }
}
