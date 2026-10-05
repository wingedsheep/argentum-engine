package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hold for Questioning
 * {3}{U}
 * Enchantment — Aura
 * Enchant creature or planeswalker
 * When this Aura enters, tap enchanted permanent and investigate.
 * Enchanted permanent doesn't untap during its controller's untap step and its activated
 * abilities can't be activated.
 *
 * Stuck in Summoner's Sanctum's lock on a creature-or-planeswalker host, plus a Clue. The
 * activation lock ([PreventActivatedAbilities] scoped to this Aura's host) covers loyalty
 * abilities too, since they're activated abilities. Tapping an already-tapped host does nothing,
 * but the investigate still happens.
 */
val HoldForQuestioning = card("Hold for Questioning") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or planeswalker\n" +
        "When this Aura enters, tap enchanted permanent and investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Enchanted permanent doesn't untap during its controller's untap step and its activated " +
        "abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedPermanent) then Effects.Investigate()
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Samuel Perin"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74004b11-53dc-475c-8d29-cf52403456cf.jpg?1783919194"
        ruling("2022-12-02", "Static and triggered abilities are unaffected by Hold for Questioning.")
        ruling("2022-12-02", "The permanent can be untapped by other spells and abilities.")
        ruling("2022-12-02", "You can enchant a permanent that is already tapped with Hold for Questioning. If you do, it doesn't become tapped again, but you will still investigate.")
        ruling("2022-12-02", "Loyalty abilities are a type of activated ability, so Hold for Questioning will prevent an enchanted planeswalker's loyalty abilities being activated.")
    }
}
