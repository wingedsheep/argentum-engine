package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tezzeret's Touch
 * {1}{U}{B}
 * Enchantment — Aura
 * Enchant artifact
 * Enchanted artifact is a creature with base power and toughness 5/5 in addition to its other types.
 * When enchanted artifact is put into a graveyard, return that card to its owner's hand.
 *
 * Mightstone's Animation's shape at 5/5: the creature type is added in layer 4 and the base P/T set
 * in layer 7b, so counters and pumps still apply on top. "Put into a graveyard" is
 * `Triggers.attached.dies()` — the same from-the-battlefield event for any permanent — looking back at
 * the Aura's last attachment, so it fires even though the Aura follows the artifact to the graveyard.
 * The return is guarded to the graveyard: a card that has already left it (or a token, which ceased
 * to exist) stays where it is.
 */
val TezzeretsTouch = card("Tezzeret's Touch") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "Enchanted artifact is a creature with base power and toughness 5/5 in addition to its other types.\n" +
        "When enchanted artifact is put into a graveyard, return that card to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    staticAbility {
        ability = GrantCardType("CREATURE", filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = SetBasePowerToughnessStatic(5, 5)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c2e917f-734e-4b8b-abc6-ea33c0fc722e.jpg?1783936732"
        ruling(
            "2017-02-09",
            "An artifact that becomes a creature due to Tezzeret's Touch can't attack unless you've " +
                "controlled it continuously since your turn began. It doesn't matter whether or not it " +
                "was a creature at that time."
        )
        ruling(
            "2017-02-09",
            "An artifact creature enchanted by Tezzeret's Touch becomes 5/5 instead of its normal power " +
                "and toughness."
        )
        ruling(
            "2017-02-09",
            "A Vehicle enchanted by Tezzeret's Touch becomes a 5/5 creature. Crewing it won't change its " +
                "power and toughness."
        )
        ruling(
            "2017-02-09",
            "If the enchanted artifact is an Equipment, it becomes unattached and it can't be attached to " +
                "anything for as long as it remains a creature."
        )
        ruling(
            "2017-02-09",
            "If Tezzeret's Touch becomes unattached from an artifact that's attacking or blocking, that " +
                "artifact will be removed from combat unless it's normally a creature or another effect " +
                "such as a crew ability is also making it a creature."
        )
    }
}
