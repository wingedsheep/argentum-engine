package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relentless X-ATM092
 * {6}
 * Artifact Creature — Robot Spider
 * 6/5
 * This creature can't be blocked except by three or more creatures.
 * {8}: Return this card from your graveyard to the battlefield tapped with a finality counter on it.
 *   (If a creature with a finality counter on it would die, exile it instead.)
 *
 * The evasion is the generalized-menace static [CantBeBlockedByFewerThan] (minBlockers = 3, like Troll
 * of Khazad-dûm). The recursion is a graveyard-activated ability (`activateFromZone = GRAVEYARD`) that
 * puts the card straight onto the battlefield tapped and stamps a finality counter; the
 * [Counters.FINALITY] counter's exile-instead-of-die replacement is handled by the engine, so no
 * extra wiring is needed for the reminder text.
 */
val RelentlessXatm092 = card("Relentless X-ATM092") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Robot Spider"
    oracleText = "This creature can't be blocked except by three or more creatures.\n" +
        "{8}: Return this card from your graveyard to the battlefield tapped with a finality counter " +
        "on it. (If a creature with a finality counter on it would die, exile it instead.)"
    power = 6
    toughness = 5

    staticAbility {
        ability = CantBeBlockedByFewerThan(3)
    }

    activatedAbility {
        cost = Costs.Mana("{8}")
        effect = Effects.Composite(
            Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true),
            Effects.AddCounters(Counters.FINALITY, 1, EffectTarget.Self)
        )
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "268"
        artist = "Kevin Glint"
        flavorText = "\"Let's get the hell out of here!\"\n—Zell Dincht"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e09ee4c9-85ef-4d1e-864b-d659b8e8f51d.jpg?1748706789"
    }
}
