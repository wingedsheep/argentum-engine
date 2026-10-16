package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gunner Conscript — Fallout #77
 * {1}{G} · Creature — Human Mercenary · 2/2
 *
 * Trample
 * This creature gets +1/+1 for each Aura and Equipment attached to it.
 * When this creature dies, if it was enchanted, create a Junk token.
 * When this creature dies, if it was equipped, create a Junk token.
 *
 * The two dies triggers are self-subject triggers narrowed by `enchanted()` / `equipped()`.
 * On a battlefield departure the trigger matcher answers those predicates from the
 * zone change's last-known snapshot (`wasEnchanted` / `wasEquipped`), frozen before the
 * attachments fall off — so a creature that was both enchanted and equipped makes two Junk.
 * The condition is last-known information, so the intervening-if re-check at resolution
 * can't change its answer.
 */
val GunnerConscript = card("Gunner Conscript") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Mercenary"
    power = 2
    toughness = 2
    oracleText = "Trample\nThis creature gets +1/+1 for each Aura and Equipment attached to it.\nWhen this creature dies, if it was enchanted, create a Junk token.\nWhen this creature dies, if it was equipped, create a Junk token."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.attachmentsOnSelf(),
            toughnessBonus = DynamicAmounts.attachmentsOnSelf()
        )
    }

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.enchanted()).dies()
        effect = Effects.CreateJunk()
    }

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.equipped()).dies()
        effect = Effects.CreateJunk()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Miguel Mercado"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f6d444f-9ffb-4dc2-b668-738d45aec2c3.jpg?1783912398"
        ruling("2024-03-08", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to Gunner Conscript may become lethal if an Aura or Equipment attached to it leaves the battlefield or becomes unattached from Gunner Conscript during that turn.")
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
