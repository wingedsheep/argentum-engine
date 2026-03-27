package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kindlespark Duo
 * {2}{R}
 * Creature — Lizard Otter
 * 1/3
 * {T}: This creature deals 1 damage to target opponent.
 * Whenever you cast a noncreature spell, untap this creature.
 */
val KindlesparkDuo = card("Kindlespark Duo") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Lizard Otter"
    power = 1
    toughness = 3
    oracleText = "{T}: This creature deals 1 damage to target opponent.\nWhenever you cast a noncreature spell, untap this creature."

    activatedAbility {
        cost = AbilityCost.Tap
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.DealDamage(1, opponent)
    }

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Daren Bader"
        flavorText = "\"We burn like fire!\" cackled the lizard. \"And strike like lightning!\" the otter thundered."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a839fba3-1b66-4dd1-bf43-9b015b44fc81.jpg?1721426652"
    }
}
