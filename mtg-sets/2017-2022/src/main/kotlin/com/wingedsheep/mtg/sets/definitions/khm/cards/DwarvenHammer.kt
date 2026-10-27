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
 * Dwarven Hammer
 * {2}{R}
 * Artifact — Equipment
 * When this Equipment enters, you may pay {2}. If you do, create a 2/1 red Dwarf Berserker
 * creature token, then attach this Equipment to it.
 * Equipped creature gets +3/+0 and has trample.
 * Equip {3}
 *
 * Same shell as Draugr's Helm / Valkyrie's Sword: [Effects.MayPay] gates the token, published to
 * [CREATED_TOKENS] and read back by [Effects.AttachEquipment].
 */
val DwarvenHammer = card("Dwarven Hammer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you may pay {2}. If you do, create a 2/1 red Dwarf Berserker creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +3/+0 and has trample.\n" +
        "Equip {3}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.CreateToken(
                power = 2,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Dwarf", "Berserker"),
                imageUri = "https://cards.scryfall.io/normal/front/1/4/142bbbe1-4446-4d80-b82d-0df8ef17eac1.jpg?1783928079",
            ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
        )
    }

    staticAbility {
        ability = ModifyStats(3, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9cc132f-d4c1-4bc4-9c9e-f63ab5e7d094.jpg?1783928230"
        ruling("2021-02-05", "You decide whether to pay {2} as the enters-the-battlefield ability resolves. If you do, you immediately create the Dwarf Berserker creature token and attach Dwarven Hammer to it. No players may respond to your decision to pay or not, and no player may take actions during this process.")
        ruling("2021-02-05", "The Dwarf Berserker creature token enters the battlefield as a 2/1 creature. Any abilities that trigger when a creature with a certain power enters the battlefield will see the token enter as a 2/1 creature.")
    }
}
