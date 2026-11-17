package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rally the Ancestors — Fate Reforged #22 (canonical / earliest real printing)
 * {X}{W}{W} · Instant
 *
 * Return each creature card with mana value X or less from your graveyard to the battlefield.
 * Exile those creatures at the beginning of your next upkeep. Exile Rally the Ancestors.
 *
 * The Lukka, Wayward Bonder −2 shape widened to a group: every creature card in your graveyard
 * whose mana value is at most the X paid ([GameObjectFilter.manaValueAtMostX], read at
 * resolution) is moved to the battlefield as a tracked collection, and a delayed trigger on your
 * own upkeep (`fireOnPlayer = You`) from the next turn on (`NEXT_TURN` — the current turn's upkeep
 * has always begun by the time an instant resolves, Pact of Negation) exiles exactly those
 * objects. A returned creature that has since left the battlefield is a new object the trigger
 * leaves alone (CR 400.7). "Exile Rally the Ancestors" is [selfExile].
 */
val RallyTheAncestors = card("Rally the Ancestors") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Return each creature card with mana value X or less from your graveyard to the " +
        "battlefield. Exile those creatures at the beginning of your next upkeep. Exile Rally the Ancestors."

    spell {
        effect = Effects.Pipeline {
            val eligible = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature.manaValueAtMostX())
            )
            val returned = moveTracked(
                eligible,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                name = "rallyTheAncestorsReturned",
            )
            ifNotEmpty(returned) {
                run(Effects.CreateDelayedTrigger(
                    step = Step.UPKEEP,
                    fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                    timing = DelayedTriggerTiming.NEXT_TURN,
                    effect = Effects.Pipeline {
                        move(returned, CardDestination.ToZone(Zone.EXILE))
                    },
                    carryCollections = listOf(returned.key),
                ))
            }
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Nils Hamm"
        flavorText = "\"The family is a tree, and in times of need, every branch can be a weapon.\"\n—Daghatar the Adamant"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d9efacb-9d7c-41f4-9b11-59df1be50e11.jpg?1783938710"
    }
}
