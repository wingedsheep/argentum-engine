package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Falcon's Wing Harness — Marvel Super Heroes #53
 * {1}{U} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +1/+1 and has flying and ward {1}.
 * Equip {2}{U}
 *
 * The ETB is the Sovereign's Macuahuitl / Super Suit idiom — [Effects.AttachEquipment] on a
 * "target creature you control". The three grants are the canonical Equipment statics scoped to
 * [Filters.EquippedCreature]: [ModifyStats], [GrantKeyword] for flying, and [GrantWard] with a
 * [WardCost.Mana] of `{1}` (the Lavaspur Boots shape — the engine synthesizes both the ward
 * keyword display and the enforcement trigger from the static).
 */
val FalconsWingHarness = card("Falcon's Wing Harness") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+1 and has flying and ward {1}. (Whenever equipped creature " +
        "becomes the target of a spell or ability an opponent controls, counter it unless that " +
        "player pays {1}.)\n" +
        "Equip {2}{U} ({2}{U}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        description = "When this Equipment enters, attach it to target creature you control."
    }

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"), Filters.EquippedCreature)
    }

    equipAbility("{2}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20e93f86-9e20-4e08-9bf7-ae6ebebf6876.jpg?1783902960"
    }
}
