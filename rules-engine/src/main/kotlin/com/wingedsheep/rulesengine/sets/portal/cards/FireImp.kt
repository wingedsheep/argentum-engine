package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object FireImp {
    val definition = CardDefinition.creature(
        name = "Fire Imp",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype.IMP),
        power = 2,
        toughness = 1,
        oracleText = "When Fire Imp enters the battlefield, it deals 2 damage to target creature.",
        metadata = ScryfallMetadata(
            collectorNumber = "126",
            rarity = Rarity.UNCOMMON,
            artist = "DiTerlizzi",
            imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7edaf3-7941-4085-bdbc-e5c9832b6444.jpg",
            scryfallId = "ea7edaf3-7941-4085-bdbc-e5c9832b6444",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Fire Imp") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = DealDamageEffect(2, EffectTarget.TargetCreature)
        )
    }
}
