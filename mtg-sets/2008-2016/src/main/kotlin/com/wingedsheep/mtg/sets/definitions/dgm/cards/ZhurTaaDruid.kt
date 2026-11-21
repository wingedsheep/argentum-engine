package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zhur-Taa Druid
 * {R}{G}
 * Creature — Human Druid
 * 1/1
 * {T}: Add {G}.
 * Whenever you tap this creature for mana, it deals 1 damage to each opponent.
 *
 * Canonical printing: Dragon's Maze (2013), the card's earliest printing. The trigger adds no mana,
 * so it is not a mana ability: it uses the stack after the mana is added (the same
 * `tappedForMana` trigger as Forbidden Orchard).
 */
val ZhurTaaDruid = card("Zhur-Taa Druid") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Druid"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {G}.\nWhenever you tap this creature for mana, it deals 1 damage to each opponent."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.self.tappedForMana()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever you tap this creature for mana, it deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Mark Winters"
        flavorText = "\"Only the decadent think magic should be pristine and without cost.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd565782-8b2f-4b9f-a62d-4af60af20a82.jpg?1783940018"
    }
}
