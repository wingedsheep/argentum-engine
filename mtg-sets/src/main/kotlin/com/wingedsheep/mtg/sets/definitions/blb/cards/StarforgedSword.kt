package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.gift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GiftKind
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.targets.EffectTarget

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
 * The gift is promised as you cast (CR 702.174a) — `gift(...)` supplies both the additional cost
 * and the "they create a tapped Fish" enters ability. The printed attach ability is an
 * intervening-if trigger (CR 603.4) on the same promise, so an unpromised cast never triggers and
 * never asks for a creature to attach to — which is what CR 702.174m requires: targets belonging
 * to a gift-gated part of an ability are chosen only if the gift was promised.
 */
val StarforgedSword = card("Starforged Sword") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, when it enters, they create a tapped 1/1 blue Fish creature token.)\nWhen this Equipment enters, if the gift was promised, attach this Equipment to target creature you control.\nEquipped creature gets +3/+3 and loses flying.\nEquip {3}"

    gift(GiftKind.TAPPED_FISH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.GiftWasPromised
        target = Targets.CreatureYouControl
        effect = Effects.AttachEquipment(EffectTarget.ContextTarget(0))
    }

    // Equipped creature gets +3/+3
    staticAbility {
        ability = ModifyStats(+3, +3, Filters.EquippedCreature)
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
