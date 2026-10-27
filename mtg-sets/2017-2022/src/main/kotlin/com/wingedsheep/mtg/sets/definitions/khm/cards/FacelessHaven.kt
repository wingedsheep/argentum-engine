package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faceless Haven
 * Snow Land
 * {T}: Add {C}.
 * {S}{S}{S}: This land becomes a 4/3 creature with vigilance and all creature types until end of
 * turn. It's still a land.
 *
 * "All creature types" rides on a granted Changeling keyword, which the layer system expands to
 * every creature type (the Mutavault token / Soulstone Sanctuary shape). BecomeCreature keeps the
 * existing Snow Land types, so it's still a land.
 */
val FacelessHaven = card("Faceless Haven") {
    manaCost = ""
    typeLine = "Snow Land"
    oracleText = "{T}: Add {C}.\n" +
        "{S}{S}{S}: This land becomes a 4/3 creature with vigilance and all creature types until end of turn. " +
        "It's still a land. ({S} can be paid with one mana from a snow source.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{S}{S}{S}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 3,
            keywords = setOf(Keyword.VIGILANCE, Keyword.CHANGELING),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Titus Lunter"
        flavorText = "It is everywhere and nowhere, a place unbound by logic, just beyond the real."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3cd82e5-6072-4334-a493-01ca4ad6b4eb.jpg?1783928179"
    }
}
