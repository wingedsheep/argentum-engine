package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elven Bow
 * {G}
 * Artifact — Equipment
 * When this Equipment enters, you may pay {2}. If you do, create a 1/1 green Elf Warrior creature
 * token, then attach this Equipment to it.
 * Equipped creature gets +1/+2 and has reach.
 * Equip {3}
 *
 * "You may pay {2}. If you do, …" is an optional payment as the trigger resolves
 * ([Effects.MayPay] → `Gate.MayPay`). The payoff is the Kyoshi Battle Fan shell: the token is
 * published to [CREATED_TOKENS] and [Effects.AttachEquipment] reads it back.
 */
val ElvenBow = card("Elven Bow") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you may pay {2}. If you do, create a 1/1 green Elf Warrior creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +1/+2 and has reach.\n" +
        "Equip {3}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Elf", "Warrior"),
                imageUri = "https://cards.scryfall.io/normal/front/1/1/118d0655-5719-4512-8bc1-fe759669811b.jpg?1783928078",
            ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
        )
    }

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Dallas Williams"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34c599c7-bcc1-4005-b830-1fa4811af66e.jpg?1783928216"
        ruling("2021-02-05", "You decide whether to pay {2} as the enters-the-battlefield ability resolves. If you do, you immediately create the Elf Warrior creature token and attach Elven Bow to it. No players may respond to your decision to pay or not, and no player may take actions during this process.")
    }
}
