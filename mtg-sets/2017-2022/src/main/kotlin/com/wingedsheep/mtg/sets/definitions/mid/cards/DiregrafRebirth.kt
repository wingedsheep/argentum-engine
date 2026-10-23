package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Diregraf Rebirth
 * {3}{B}{G}
 * Sorcery
 * This spell costs {1} less to cast for each creature that died this turn.
 * Return target creature card from your graveyard to the battlefield.
 * Flashback {5}{B}{G}
 *
 * The discount counts every creature that died this turn under any player's control
 * (`creaturesDiedThisTurn(Player.Each)`, as Death-Rattle Oni) and reduces only the generic part.
 * The flashback cast is priced through the same self-cast reduction pipeline, so it applies to
 * {5}{B}{G} too.
 */
val DiregrafRebirth = card("Diregraf Rebirth") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast for each creature that died this turn.\n" +
        "Return target creature card from your graveyard to the battlefield.\n" +
        "Flashback {5}{B}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.creaturesDiedThisTurn(Player.Each)),
            ),
        )
    }

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(
            target = creature,
            destination = Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
        )
    }

    keywordAbility(KeywordAbility.flashback("{5}{B}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b51ed1ea-661e-49c6-9751-1eb7bb651bb1.jpg?1783925565"
    }
}
