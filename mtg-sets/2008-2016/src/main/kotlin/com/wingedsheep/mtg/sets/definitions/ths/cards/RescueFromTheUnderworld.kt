package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rescue from the Underworld
 * {4}{B}
 * Instant
 * As an additional cost to cast this spell, sacrifice a creature.
 * Choose target creature card in your graveyard. Return that card and the sacrificed card to the
 * battlefield under your control at the beginning of your next upkeep. Exile Rescue from the Underworld.
 *
 * Both cards are gathered as the spell resolves and carried onto the upkeep delayed trigger
 * (`carryCollections`), so either one that has left its zone by then is a new object and stays put.
 * `CardSource.SacrificedThisWay` follows the sacrificed card into whatever public zone it went to —
 * a replacement that exiled it instead of putting it into the graveyard still has it come back —
 * and a sacrificed token, gone entirely, never returns. The spell exiles itself as it resolves.
 */
val RescueFromTheUnderworld = card("Rescue from the Underworld") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Choose target creature card in your graveyard. Return that card and the sacrificed card to " +
        "the battlefield under your control at the beginning of your next upkeep. Exile Rescue from " +
        "the Underworld."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets, name = "rescueChosen")
            val sacrificed = gather(CardSource.SacrificedThisWay, name = "rescueSacrificed")
            run(Effects.CreateDelayedTrigger(
                step = Step.UPKEEP,
                fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                effect = Effects.Pipeline {
                    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
                    move(sacrificed, CardDestination.ToZone(Zone.BATTLEFIELD))
                },
                carryCollections = listOf(chosen.key, sacrificed.key)
            ))
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e46aa9c-7a29-4eb4-bc44-a201c22824d2.jpg?1783939771"

        ruling(
            "2013-09-15",
            "If either the sacrificed creature or the target creature card leaves the graveyard before the delayed triggered ability resolves during your next upkeep, it won't return."
        )
        ruling(
            "2013-09-15",
            "However, if the sacrificed creature is put into another public zone instead of the graveyard, perhaps because it's your commander or because of another replacement effect, it will return to the battlefield from the zone it went to."
        )
    }
}
