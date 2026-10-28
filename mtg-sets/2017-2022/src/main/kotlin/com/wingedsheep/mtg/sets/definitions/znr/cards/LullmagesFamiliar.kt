package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Lullmage's Familiar — Zendikar Rising #227
 * {1}{G}{U} · Creature — Beast · 2/4
 *
 * {T}: Add {G} or {U}.
 * Whenever you cast a kicked spell, you gain 2 life.
 */
val LullmagesFamiliar = card("Lullmage's Familiar") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Beast"
    oracleText = "{T}: Add {G} or {U}.\nWhenever you cast a kicked spell, you gain 2 life."
    power = 2
    toughness = 4

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Gabor Szikszai"
        flavorText = "\"They store energy in special gastric organs ... but I guess you'd probably rather have " +
            "me clear the path than ramble about guts.\"\n—Fahar, expedition wizard"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b31a81e8-df0e-4540-93c1-c30c31ea9be9.jpg?1783929324"
        ruling(
            "2020-09-25",
            "The last ability of Lullmage's Familiar triggers when you cast a kicked spell even if you didn't " +
                "spend mana from its first ability to cast that spell."
        )
    }
}
