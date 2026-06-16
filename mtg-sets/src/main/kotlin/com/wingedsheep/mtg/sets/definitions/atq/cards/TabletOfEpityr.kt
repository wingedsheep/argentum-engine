package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Tablet of Epityr
 * {1}
 * Artifact
 * Whenever an artifact you control is put into a graveyard from the battlefield, you may pay {1}.
 * If you do, you gain 1 life.
 */
val TabletOfEpityr = card("Tablet of Epityr") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever an artifact you control is put into a graveyard from the battlefield, " +
        "you may pay {1}. If you do, you gain 1 life."

    // "an artifact you control" includes this artifact itself, so the trigger uses ANY binding.
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = MayPayManaEffect(ManaCost.parse("{1}"), Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Christopher Rush"
        flavorText = "Originally considered the work of Urza, this tablet was created by forgers seeking to imitate Urza's masterpieces."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d7a2718-301f-4191-b348-0c44c7c07d43.jpg?1562917978"
    }
}
