package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Neglected Heirloom // Ashmouth Blade (Shadows over Innistrad #260 — the card's earliest printing;
 * also reprinted in Innistrad Remastered)
 * {1}
 * Artifact — Equipment // Artifact — Equipment
 *
 * Front — Neglected Heirloom ({1}, Artifact — Equipment)
 *   Equipped creature gets +1/+1.
 *   When equipped creature transforms, transform this Equipment.
 *   Equip {1}
 *
 * Back — Ashmouth Blade (Artifact — Equipment)
 *   Equipped creature gets +3/+3 and has first strike.
 *   Equip {3}
 *
 * Implementation:
 *  - Both faces are plain Equipment: [ModifyStats] and [GrantKeyword] default to
 *    `GroupFilter.attachedCreature()`, and `equipAbility(...)` wires the equip cost per face.
 *  - "When **equipped creature** transforms" is `Triggers.transforms(binding =
 *    `[TriggerBinding.ATTACHED]`)` — the Equipment watches the permanent it's attached to, the same
 *    binding behind "whenever equipped creature deals combat damage" (Goldvein Pick) and "becomes
 *    untapped" (Fishing Pole). A [TransformEffect] flips the permanent in place, so the Equipment is
 *    still attached when the event fires. The trigger takes no direction filter: it fires on a flip
 *    either way, which matches the werewolf pairs this Equipment was printed for.
 *  - Per the printed ruling, nothing card-specific handles the "equipped creature transforms into a
 *    *noncreature* permanent" case — the attachment state-based action unattaches the Equipment
 *    before this trigger resolves, and it still transforms.
 */

private val NeglectedHeirloomFront = card("Neglected Heirloom") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "When equipped creature transforms, transform this Equipment.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1)
    }

    triggeredAbility {
        trigger = Triggers.transforms(binding = TriggerBinding.ATTACHED)
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this Equipment."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Volkan Baǵa"
        flavorText = "\"There is a rich history in a blade like this.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cc2ac21-d31f-4a0d-956a-dabb6e1f3b5a.jpg?1783937716"
        ruling(
            "2016-04-08",
            "When Neglected Heirloom transforms, it remains attached to the creature it's attached " +
                "to. If the equipped creature transforms into a noncreature permanent, Neglected " +
                "Heirloom will become unattached before it transforms into Ashmouth Blade."
        )
    }
}

private val AshmouthBlade = card("Ashmouth Blade") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+3 and has first strike.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(powerBonus = 3, toughnessBonus = 3)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Volkan Baǵa"
        flavorText = "\"Its previous owners had some stories to share, I'm certain.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/back/5/c/5cc2ac21-d31f-4a0d-956a-dabb6e1f3b5a.jpg?1783937716"
    }
}

val NeglectedHeirloom: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = NeglectedHeirloomFront,
    backFace = AshmouthBlade,
)
