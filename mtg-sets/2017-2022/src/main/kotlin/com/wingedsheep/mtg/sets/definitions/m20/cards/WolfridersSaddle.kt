package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wolfrider's Saddle
 * {3}{R}
 * Artifact — Equipment
 * When this Equipment enters, create a 2/2 green Wolf creature token, then attach this Equipment to it.
 * Equipped creature gets +1/+1 and can't be blocked by more than one creature.
 * Equip {3}
 *
 * The enters trigger is the Giant's Amulet shell without the optional payment: the token is
 * published to [CREATED_TOKENS] and [Effects.AttachEquipment] reads it back in the same resolution.
 */
val WolfridersSaddle = card("Wolfrider's Saddle") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 2/2 green Wolf creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +1/+1 and can't be blocked by more than one creature.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            imageUri = "https://cards.scryfall.io/normal/front/b/d/bd05e304-1a16-436d-a05c-4a38a839759b.jpg?1783932856",
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba18cc9a-411d-45c6-bcc3-9e02ef5e2d92.jpg?1783932953"
        ruling("2019-07-12", "The Wolf token that you create enters the battlefield as a 2/2 creature. Any abilities that trigger when a creature with a certain power enters the battlefield will see the token enter as a 2/2 creature before Wolfrider's Saddle becomes attached.")
        ruling("2019-07-12", "No player may take any actions between the time you create the Wolf token and the time Wolfrider's Saddle becomes attached to it.")
        ruling("2019-07-12", "If the equipped creature has menace, it can't be blocked at all.")
    }
}
