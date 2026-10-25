package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Monk Class {W}{U}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * The second spell you cast each turn costs {1} less to cast.
 *
 * {W}{U}: Level 2
 * When this Class becomes level 2, return up to one target nonland permanent to its owner's hand.
 *
 * {1}{W}{U}: Level 3
 * At the beginning of your upkeep, exile the top card of your library. For as long as it remains
 * exiled, it has "You may cast this card from exile as long as you've cast another spell this turn."
 *
 * Level 1 is Uthros Psionicist's Nth-spell reduction (`NthOfTypePerTurn(2)`, which counts the spell
 * being cast). Level 3 is the impulse-exile + conditional permission shape: a [MayPlayExpiry.Permanent]
 * grant (lasts as long as the card stays in exile), `nonLandOnly` because the granted ability lets
 * you *cast* the card, never play it as a land, gated on `YouCastSpellsThisTurn(atLeast = 1)` — the
 * gate is checked before the exiled card is cast, so the one spell it requires is another spell.
 */
val MonkClass = card("Monk Class") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "The second spell you cast each turn costs {1} less to cast.\n" +
        "{W}{U}: Level 2 — When this Class becomes level 2, return up to one target nonland " +
        "permanent to its owner's hand.\n" +
        "{1}{W}{U}: Level 3 — At the beginning of your upkeep, exile the top card of your library. " +
        "For as long as it remains exiled, it has \"You may cast this card from exile as long as " +
        "you've cast another spell this turn.\""

    // Level 1: the second spell you cast each turn costs {1} less.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.NthOfTypePerTurn(2),
        )
    }

    // Level 2: bounce up to one target nonland permanent.
    classLevel(2, "{W}{U}") {
        triggeredAbility {
            trigger = Triggers.self.enters()
            val permanent = target(TargetFilter.NonlandPermanent, optional = true)
            effect = Effects.ReturnToHand(permanent)
            description = "When this Class becomes level 2, return up to one target nonland " +
                "permanent to its owner's hand."
        }
    }

    // Level 3: upkeep exile, castable while it stays exiled once you've cast another spell this turn.
    classLevel(3, "{1}{W}{U}") {
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.Pipeline {
                val exiledCard = gather(CardSource.TopOfLibrary(1))
                exile(exiledCard)
                run(Effects.GrantMayPlayFromExile(
                    from = exiledCard,
                    expiry = MayPlayExpiry.Permanent,
                    condition = Conditions.YouCastSpellsThisTurn(atLeast = 1),
                    nonLandOnly = true,
                ))
            }
            description = "At the beginning of your upkeep, exile the top card of your library. " +
                "For as long as it remains exiled, it has \"You may cast this card from exile as " +
                "long as you've cast another spell this turn.\""
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2edd708-46ee-4963-b7e6-b631616d78fe.jpg?1783926446"
        ruling("2021-07-23", "If Monk Class is the first spell you cast, the next spell is the second spell and costs {1} less. If Monk Class is the second (or later) spell you cast, its bonus won't apply until the next turn.")
        ruling("2021-07-23", "If you have more than one Monk Class on the battlefield, their abilities are cumulative. For example, if you control two Monk Classes, the second spell you cast each turn costs {2} less to cast.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "You can't activate the first level ability of a Class unless that Class is level 1. Similarly, you can't activate the second level ability of a Class unless that Class is level 2.")
    }
}
