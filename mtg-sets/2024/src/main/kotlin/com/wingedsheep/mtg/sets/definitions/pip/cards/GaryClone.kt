package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val GaryClone = card("Gary Clone") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Citizen"
    power = 1
    toughness = 3
    oracleText = "Squad {2} (As an additional cost to cast this spell, you may pay {2} any number of times. When this creature enters, create that many tokens that are copies of it.)\nWhenever this creature attacks, each creature you control named Gary Clone gets +1/+0 until end of turn."
    squad("{2}")
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl.named("Gary Clone"),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Nicholas Gregory"
        flavorText = "\"Gary!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a45346ac-54a9-4fda-bde7-92ff75ac34f7.jpg?1783912419"
        ruling("2024-03-08", "You may pay the squad cost any number of times. You will create a token that is a copy of that permanent for each time you paid the squad cost. If you paid the squad cost multiple times, the tokens will all enter the battlefield simultaneously.")
        ruling("2024-03-08", "If the spell is countered, the squad ability will not trigger, and no tokens will be created.")
        ruling("2024-03-08", "If the spell resolves but the creature with squad leaves the battlefield before its squad ability resolves, you'll still create the token copies.")
        ruling("2024-03-08", "The tokens created by the squad ability aren't \"cast,\" so any abilities that trigger when a spell is cast won't trigger for the copies.")
        ruling("2024-03-08", "If, for some reason, the creature doesn't have the squad ability when it's on the battlefield, the ability won't trigger, even if you've paid the squad cost one or more times.")
    }
}
