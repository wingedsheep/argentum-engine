package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainLifeInsteadOfLosingLife
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.LifeLossOrigin
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val StrongTheBrutishThespian = card("Strong, the Brutish Thespian") {
    manaCost = "{4}{G}{G}"
    typeLine = "Legendary Creature — Mutant Berserker"
    power = 7
    toughness = 7
    oracleText = "Ward {2}\n" +
        "Enrage — Whenever Strong is dealt damage, you get three rad counters and put three +1/+1 counters on Strong.\n" +
        "You gain life rather than lose life from radiation."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.AddCounters(CounterType.RAD, 3, EffectTarget.Controller) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    replacementEffect(GainLifeInsteadOfLosingLife(origin = LifeLossOrigin.RADIATION))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Jason Rainville"
        flavorText = "\"Strong learn secret to human power from Mack Beth.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bc79ed6-057b-439f-b2cd-58d5bcd8b551.jpg?1783912395"
    }
}
