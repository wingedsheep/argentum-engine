package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Smoke Shroud
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has flying.
 * When a Ninja you control enters, you may return this card from your graveyard to the
 * battlefield attached to that creature.
 *
 * The graveyard-zone return is the Dragon Wings shape (Scourge's Dragon cycle): a
 * `triggerZone = GRAVEYARD` enters trigger whose "may" returns the Aura attached to the
 * triggering creature.
 */
val SmokeShroud = card("Smoke Shroud") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has flying.\n" +
        "When a Ninja you control enters, you may return this card from your graveyard to the " +
        "battlefield attached to that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Ninja").youControl()).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            effect = Effects.ReturnSelfToBattlefieldAttached(),
            descriptionOverride = "Return Smoke Shroud attached to this Ninja?",
            sourceRequiredZone = Zone.GRAVEYARD,
            inlineOnTrigger = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Micah Epstein"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e07c876e-ae1c-4b54-8d8a-9f3fd76ca64c.jpg?1783933137"
    }
}
