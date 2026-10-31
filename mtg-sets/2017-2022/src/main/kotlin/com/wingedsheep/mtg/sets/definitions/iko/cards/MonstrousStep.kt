package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Monstrous Step — Ikoria: Lair of Behemoths #166
 * {4}{G} · Sorcery
 *
 * Target creature gets +7/+7 until end of turn. Up to one other target creature blocks it this
 * turn if able.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * The Hunt Down shape (`Effects.ForceBlock(blocker, attacker)`) after a pump. The blocker is an
 * optional [TargetOther] requirement so it can't be the pumped creature; left unchosen, the force
 * block simply does nothing.
 */
val MonstrousStep = card("Monstrous Step") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +7/+7 until end of turn. Up to one other target creature " +
        "blocks it this turn if able.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val pumped = target(TargetFilter.Creature)
        val blocker = target(TargetOther(TargetObject(filter = TargetFilter.Creature)), optional = true)
        effect = Effects.ModifyStats(7, 7, pumped) then Effects.ForceBlock(blocker, attacker = pumped)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Chris Seaman"
        flavorText = "Life is best taken one step at a time."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/893f300b-a823-4156-90f2-0636e9f499b0.jpg?1783931032"
        ruling("2020-04-17", "If the second target creature can’t block the first target creature for any reason (such as being tapped), then it doesn’t block. If there’s a cost associated with having it block the creature, the player isn’t forced to pay that cost, so it doesn’t have to block in that case either. That creature is free to block another creature or to not block at all.")
        ruling("2020-04-17", "If a creature is required to block a creature with menace, another creature must also block that creature if able. If none can, the creature that’s required to block can block another creature or not block at all.")
        ruling("2020-04-17", "Creatures other than the second target creature may still block the first target creature if able.")
    }
}
