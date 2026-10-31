package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ancestral Blade
 * {1}{W}
 * Artifact — Equipment
 * When this Equipment enters, create a 1/1 white Soldier creature token, then attach this
 *   Equipment to it.
 * Equipped creature gets +1/+1.
 * Equip {1}
 *
 * Same create-then-attach shell as Mask of Immolation: the token publishes its id to
 * [CREATED_TOKENS] and the Equipment attaches to the first one (Doubling Season ruling).
 */
val AncestralBlade = card("Ancestral Blade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 1/1 white Soldier creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +1/+1.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/a/4/a491d9da-a58d-4423-b565-c5a99ef63132.jpg?1783932857"
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2ba18114-af6c-48cd-82c9-eb6541d566bf.jpg?1783933034"
        ruling(
            "2020-11-10",
            "The Soldier token that you create enters the battlefield as a 1/1 creature. Any abilities that trigger when a creature with a certain power enters the battlefield will see the token enter as a 1/1 creature. Static abilities that affect the Soldier's power and toughness may change this, but Ancestral Blade will be attached to the Soldier before the game checks to see if it has toughness 0 or less. For example, if Illness in the Ranks (\"Creature tokens get -1/-1\") is on the battlefield, the Soldier will enter as a 0/0 creature, but then Ancestral Blade will make it a 1/1 and the token will survive."
        )
        ruling(
            "2020-11-10",
            "No player may take any actions between the time you create the Soldier token and the time Ancestral Blade becomes attached to it."
        )
        ruling(
            "2020-11-10",
            "If the triggered ability causes two Soldiers to be created (due to an effect such as that of Doubling Season), Ancestral Blade becomes attached to one of them."
        )
    }
}
