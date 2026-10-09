package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.WardCost

val SuChiCaveGuard = card("Su-Chi Cave Guard") {
    manaCost = "{8}"
    typeLine = "Artifact Creature — Construct"
    power = 8
    toughness = 8
    oracleText = "Vigilance\nWard {4} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {4}.)\nWhen this creature dies, add eight {C}. Until end of turn, you don't lose this mana as steps and phases end."

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{4}")))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.AddColorlessMana(8, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb04dad2-4561-4cef-9cee-80d6f1a44bee.jpg?1783920012"
    }
}
