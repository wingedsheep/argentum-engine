package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spider-Slayer, Hatred Honed — Marvel's Spider-Man #175
 * {2} · Legendary Artifact Creature — Human Villain · 2/1
 *
 * Whenever Spider-Slayer deals damage to a Spider, destroy that creature.
 * {6}, Exile this card from your graveyard: Create two tapped 1/1 colorless Robot
 * artifact creature tokens with flying.
 *
 * The first ability uses `RecipientFilter.Matching` on a self deals-damage trigger — the same
 * shape as East-Mark Cavalier / Mauhur (now wired in `TriggerMatcher.matchesDealsDamageTrigger`).
 */
val SpiderSlayerHatredHoned = card("Spider-Slayer, Hatred Honed") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Human Villain"
    power = 2
    toughness = 1
    oracleText = "Whenever Spider-Slayer deals damage to a Spider, destroy that creature.\n" +
        "{6}, Exile this card from your graveyard: Create two tapped 1/1 colorless Robot " +
        "artifact creature tokens with flying."

    // Whenever Spider-Slayer deals damage to a Spider, destroy that creature.
    triggeredAbility {
        trigger = Triggers.dealsDamage(
            recipient = RecipientFilter.Matching(
                GameObjectFilter.Creature.withAnySubtype("Spider")
            )
        )
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    // {6}, Exile this card from your graveyard: Create two tapped 1/1 flying Robot tokens.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Robot"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            tapped = true,
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "David Álvarez"
        flavorText = "\"With each attempt on Spider-Man's life, my father came closer to ending " +
            "the menace. It is my pleasure to finish the job.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac37ca6f-a6ee-4dfb-949f-2562f98d09d0.jpg?1783905300"
    }
}
