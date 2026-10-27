package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Giant's Amulet
 * {U}
 * Artifact — Equipment
 * When this Equipment enters, you may pay {3}{U}. If you do, create a 4/4 blue Giant Wizard
 * creature token, then attach this Equipment to it.
 * Equipped creature gets +0/+1 and has "This creature has hexproof as long as it's untapped."
 * Equip {2}
 *
 * The enters trigger is the Elven Bow shell: an optional payment as the trigger resolves
 * ([Effects.MayPay]), the token published to [CREATED_TOKENS], and [Effects.AttachEquipment]
 * reading it back. The granted "hexproof as long as it's untapped" is a hexproof grant gated by
 * [Conditions.EntityMatches] on [EffectTarget.EquippedCreature] being untapped (Tenza, Godo's
 * Maul's shape), re-evaluated continuously so it switches off the moment the creature taps.
 */
val GiantsAmulet = card("Giant's Amulet") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you may pay {3}{U}. If you do, create a 4/4 blue Giant Wizard creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +0/+1 and has \"This creature has hexproof as long as it's untapped.\"\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{3}{U}"),
            then = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Giant", "Wizard"),
                imageUri = "https://cards.scryfall.io/normal/front/a/4/a40a0cb8-0caf-4dd1-b7d0-abbc626985d2.jpg?1783928080",
            ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
        )
    }

    staticAbility {
        ability = ModifyStats(0, 1)
    }

    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.untapped()
        )
        ability = GrantKeyword(Keyword.HEXPROOF)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab8b1ec2-9303-4722-8644-b3bc1a5c387f.jpg?1783928263"
        ruling("2021-02-05", "You decide whether to pay {3}{U} as the enters-the-battlefield ability resolves. If you do, you immediately create the Giant Wizard creature token and attach Giant's Amulet to it. No players may respond to your decision to pay or not, and no player may take actions during this process.")
        ruling("2021-02-05", "The Giant Wizard creature token enters the battlefield as a 4/4 creature. Any abilities that trigger when a creature with a certain toughness enters the battlefield will see the token enter as a 4/4 creature.")
    }
}
