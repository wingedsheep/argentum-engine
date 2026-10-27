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
 * Draugr's Helm
 * {1}{B}
 * Artifact — Equipment
 * When this Equipment enters, you may pay {2}{B}. If you do, create a 2/2 black Zombie Berserker
 * creature token, then attach this Equipment to it.
 * Equipped creature gets +2/+2 and has menace.
 * Equip {4}
 *
 * The enters trigger is the Elven Bow / Giant's Amulet shell: an optional payment as the trigger
 * resolves ([Effects.MayPay]), the token published to [CREATED_TOKENS], and
 * [Effects.AttachEquipment] reading it back.
 */
val DraugrsHelm = card("Draugr's Helm") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you may pay {2}{B}. If you do, create a 2/2 black Zombie Berserker creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +2/+2 and has menace. (It can't be blocked except by two or more creatures.)\n" +
        "Equip {4}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{B}"),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie", "Berserker"),
                imageUri = "https://cards.scryfall.io/normal/front/e/4/e4bbd379-6728-4c6b-b375-295926fe6b00.jpg?1783928080",
            ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
        )
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd4aade3-b1e3-43d1-b4a8-6b55ecdb4327.jpg?1783928251"
        ruling("2021-02-05", "You decide whether to pay {2}{B} as the enters-the-battlefield ability resolves. If you do, you immediately create the Zombie Berserker creature token and attach Draugr's Helm to it. No players may respond to your decision to pay or not, and no player may take actions during this process.")
        ruling("2021-02-05", "The Zombie Berserker creature token enters the battlefield as a 2/2 creature. Any abilities that trigger when a creature with a certain power or toughness enters the battlefield will see the token enter as a 2/2 creature.")
    }
}
