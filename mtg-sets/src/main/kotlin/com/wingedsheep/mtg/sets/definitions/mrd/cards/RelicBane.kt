package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relic Bane — Mirrodin #76 (canonical printing, only printing)
 * {1}{B}{B} · Enchantment — Aura
 *
 * Enchant artifact
 * Enchanted artifact has "At the beginning of your upkeep, you lose 2 life."
 *
 * The Aura grants the upkeep trigger to the *enchanted artifact*, not to the Aura itself — the
 * Essence Leak shape: [GrantTriggeredAbility] over [GroupFilter.attachedCreature] (scope-by-
 * attachment, so it works for any permanent type, artifacts included). Granting it rather than
 * printing the trigger on the Aura is what makes "your" mean the *artifact's* controller: the
 * granted ability is controlled by whoever controls the artifact, so stealing the artifact
 * moves the life loss with it, and Relic Bane's own controller is irrelevant to who pays.
 */
val RelicBane = card("Relic Bane") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "Enchanted artifact has \"At the beginning of your upkeep, you lose 2 life.\""

    auraTarget = Targets.Artifact

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.YourUpkeep.event,
                binding = Triggers.YourUpkeep.binding,
                effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.You)),
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Eric Peterson"
        flavorText = "A sword that has seen cowardice in battle exacts the price of honor from its wielder."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cbafa51-693b-485c-807d-64020540f16a.jpg?1783944545"
    }
}
