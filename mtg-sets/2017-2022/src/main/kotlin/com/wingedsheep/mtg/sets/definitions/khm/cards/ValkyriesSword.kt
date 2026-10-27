package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valkyrie's Sword
 * {1}{W}
 * Artifact — Equipment
 * When this Equipment enters, you may pay {4}{W}. If you do, create a 4/4 white Angel Warrior
 * creature token with flying and vigilance, then attach this Equipment to it.
 * Equipped creature gets +2/+1.
 * Equip {3}
 *
 * Same shell as Elven Bow: [Effects.MayPay] gates the token, published to [CREATED_TOKENS] and
 * read back by [Effects.AttachEquipment].
 */
val ValkyriesSword = card("Valkyrie's Sword") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you may pay {4}{W}. If you do, create a 4/4 white Angel Warrior creature token with flying and vigilance, then attach this Equipment to it.\n" +
        "Equipped creature gets +2/+1.\n" +
        "Equip {3}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{4}{W}"),
            then = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel", "Warrior"),
                keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
        )
    }

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97873c66-6bff-4d79-850c-1e2663098ef4.jpg?1783928273"
        ruling("2021-02-05", "You decide whether to pay {4}{W} as the enters-the-battlefield ability resolves. If you do, you immediately create the Angel Warrior creature token and attach Valkyrie's Sword to it. No players may respond to your decision to pay or not, and no player may take actions during this process.")
        ruling("2021-02-05", "The Angel Warrior creature token enters the battlefield as a 4/4 creature. Any abilities that trigger when a creature with a certain power or toughness enters the battlefield will see the token enter as a 4/4 creature.")
    }
}
