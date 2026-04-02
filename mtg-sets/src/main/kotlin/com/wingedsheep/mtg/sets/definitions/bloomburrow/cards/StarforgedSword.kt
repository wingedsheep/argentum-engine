package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Starforged Sword
 * {4}
 * Artifact — Equipment
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, when it enters, they create a tapped 1/1 blue Fish creature token.)
 * When this Equipment enters, if the gift was promised, attach this Equipment to
 * target creature you control.
 * Equipped creature gets +3/+3 and loses flying.
 * Equip {3}
 *
 * Note: Gift is modeled as a modal ETB trigger. Mode 1 = no gift (do nothing),
 * Mode 2 = gift (opponent gets Fish token, attach to target creature you control).
 */
val StarforgedSword = card("Starforged Sword") {
    manaCost = "{4}"
    typeLine = "Artifact — Equipment"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, when it enters, they create a tapped 1/1 blue Fish creature token.)\nWhen this Equipment enters, if the gift was promised, attach this Equipment to target creature you control.\nEquipped creature gets +3/+3 and loses flying.\nEquip {3}"

    // Gift modeled as a modal ETB triggered ability
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — do nothing
            Mode.noTarget(
                CompositeEffect(emptyList()),
                "Don't promise a gift"
            ),
            // Mode 2: Gift — opponent gets tapped Fish token, attach to target creature
            Mode.withTarget(
                CreateTokenEffect(
                    count = DynamicAmount.Fixed(1),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    controller = EffectTarget.PlayerRef(Player.EachOpponent),
                    tapped = true,
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                ).then(Effects.AttachEquipment(EffectTarget.ContextTarget(0)))
                    .then(Effects.GiftGiven()),
                Targets.CreatureYouControl,
                "Promise a gift — opponent creates a tapped 1/1 blue Fish token, attach to target creature you control"
            )
        )
    }

    // Equipped creature gets +3/+3
    staticAbility {
        effect = Effects.ModifyStats(+3, +3)
        filter = Filters.EquippedCreature
    }

    // Equipped creature loses flying
    staticAbility {
        ability = RemoveKeywordStatic(Keyword.FLYING)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c23d8e96-b972-4c6c-b0c4-b6627621f048.jpg?1721427296"
        ruling("2024-07-26", "Starforged Sword can be attached to a creature that didn't have flying to begin with.")
        ruling("2024-07-26", "If the equipped creature gains flying after Starforged Sword became attached to it, it will have flying.")
    }
}
