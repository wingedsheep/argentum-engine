package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Numai Outcast
 * {3}{B}
 * Creature — Human Samurai
 * 1/1
 * Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)
 * {B}, Pay 5 life: Regenerate this creature.
 *
 * The regeneration cost is a two-atom composite — mana plus a life payment — matching Assay's
 * `CostComposite(AtomMana, AtomPayLife)`. "Regenerate this creature" is [RegenerateEffect] on
 * [EffectTarget.Self]; there is no `Effects.Regenerate` facade, the effect class is the shipped
 * spelling (`m10/cards/CudgelTroll.kt`).
 */
val NumaiOutcast = card("Numai Outcast") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Samurai"
    power = 1
    toughness = 1
    oracleText = "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)\n" +
        "{B}, Pay 5 life: Regenerate this creature."

    keywordAbility(KeywordAbility.bushido(2))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.PayLife(5))
        effect = Effects.Regenerate(EffectTarget.Self)
        description = "{B}, Pay 5 life: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Adam Rex"
        flavorText = "\"Beware the blade of dishonor. It kills more silently than war, more quickly than age.\"\n—Sensei Golden-Tail"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b878d1c2-34ce-4cb4-9ea3-8d7cd2028484.jpg?1783944310"
    }
}
