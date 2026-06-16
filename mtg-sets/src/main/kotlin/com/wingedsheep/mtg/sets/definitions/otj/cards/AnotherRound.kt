package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Another Round
 * {X}{X}{2}{W}
 * Sorcery
 *
 * Exile any number of creatures you control, then return them to the battlefield under their
 * owner's control. Then repeat this process X more times.
 *
 * The "exile any number you control, then return them" blink is one atomic gather → choose →
 * exile (linked to this spell) → return-from-linked-exile pipeline ([Effects.Pipeline]). The
 * exiled creatures are returned simultaneously as one batch (a [CardSource.FromLinkedExile]
 * gather + a single [CardDestination.ToZone] move) under their owners' control, so they all
 * re-enter at once and any enters-the-battlefield abilities see the whole batch.
 *
 * "Then repeat this process X more times" = the process happens X + 1 times total: the body runs
 * once, then [RepeatDynamicTimesEffect] over [DynamicAmount.XValue] runs it X more times. Each
 * iteration re-gathers and re-prompts, so the player chooses anew every round. With the `{X}{X}`
 * cost, [DynamicAmount.XValue] is the chosen value of X (not the doubled mana paid), matching
 * Devastating Onslaught's `{X}{X}{R}` "create X tokens".
 */
val AnotherRound = card("Another Round") {
    manaCost = "{X}{X}{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile any number of creatures you control, then return them to the battlefield " +
        "under their owner's control. Then repeat this process X more times."

    spell {
        // One round: exile a player-chosen set of creatures you control, then return that same
        // set to the battlefield under their owners' control.
        val oneRound = Effects.Pipeline(
            descriptionOverride = "Exile any number of creatures you control, then return them to " +
                "the battlefield under their owner's control"
        ) {
            val controlled = gather(GameObjectFilter.Creature, player = Player.You)
            val chosen = chooseAnyNumber(
                from = controlled,
                useTargetingUI = true,
                prompt = "Choose any number of creatures you control to exile, then return"
            )
            exile(chosen, linkToSource = true)
            val returning = gather(source = CardSource.FromLinkedExile())
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }

        effect = Effects.Composite(
            listOf(
                oneRound,
                RepeatDynamicTimesEffect(amount = DynamicAmount.XValue, body = oneRound)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Darrell Riche"
        flavorText = "Weary travelers trade stories of the Eversaloon, an extraplanar respite that " +
            "appears at the moment it is most needed."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f8dc511-e307-4412-bb79-375a6077312d.jpg?1712355226"

        ruling("2024-04-12", "The creatures return to the battlefield simultaneously. They're new objects with no relation to their previous existence.")
        ruling("2024-04-12", "You choose which creatures to exile each time you perform the process. You don't have to exile the same creatures each time, and you can choose to exile no creatures.")
        ruling("2024-04-12", "If a creature you exile this way is a token, it ceases to exist and won't return to the battlefield.")
        ruling("2024-04-12", "Auras attached to the exiled creatures will be put into their owners' graveyards. Equipment will become unattached but remain on the battlefield. Counters on the exiled creatures will cease to exist.")
    }
}
