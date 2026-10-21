package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Narset, Enlightened Exile — March of the Machine: The Aftermath #38
 * {1}{U}{R}{W} · Legendary Creature — Human Monk · 3/4 · Mythic
 *
 * Creatures you control have prowess.
 * Whenever Narset attacks, exile target noncreature, nonland card with mana value less than
 * Narset's power from a graveyard and copy it. You may cast the copy without paying its mana cost.
 *
 * - **Prowess grant** — the Bria, Riptide Rogue shape: the PROWESS keyword (display) plus the
 *   intrinsic "+1/+1 whenever you cast a noncreature spell" triggered ability (behaviour), granted
 *   to every creature you control, Narset included (she has no printed prowess of her own).
 * - **Attack trigger** — the Arcane Proxy / Shiko chain: exile the target, copy it in exile, then
 *   `May`-cast the copy for free. A declined copy ceases to exist (CR 707.10a SBA).
 * - **"less than Narset's power"** — a strict `LT` comparison against `sourcePower()`, read when the
 *   target is chosen and re-checked on resolution, so a power drop in response makes the target
 *   illegal (ruling). If Narset has left, the source power falls back to last-known information.
 */
val NarsetEnlightenedExile = card("Narset, Enlightened Exile") {
    manaCost = "{1}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Human Monk"
    power = 3
    toughness = 4
    oracleText = "Creatures you control have prowess.\n" +
        "Whenever Narset attacks, exile target noncreature, nonland card with mana value less than " +
        "Narset's power from a graveyard and copy it. You may cast the copy without paying its mana cost."

    // "Creatures you control have prowess." — keyword (display) + the prowess trigger (behaviour).
    val yourCreatures = GroupFilter(GameObjectFilter.Creature.youControl())
    staticAbility {
        ability = GrantKeyword(Keyword.PROWESS, yourCreatures)
    }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                effect = Effects.ModifyStats(
                    power = 1,
                    toughness = 1,
                    target = EffectTarget.Self,
                ),
            ),
            filter = yourCreatures,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val exiledCard = target(
            TargetFilter(
                (GameObjectFilter.Noncreature and GameObjectFilter.Nonland).compareNumericProperty(
                    CardNumericProperty.MANA_VALUE,
                    ComparisonOperator.LT,
                    DynamicAmounts.sourcePower(),
                ),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.Pipeline {
            run(Effects.Move(exiledCard, Zone.EXILE))
            val copy = copyCard(exiledCard)
            run(Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(copy),
                descriptionOverride = "You may cast the copy without paying its mana cost.",
            ))
        }
        description = "Whenever Narset attacks, exile target noncreature, nonland card with mana value " +
            "less than Narset's power from a graveyard and copy it. You may cast the copy without paying " +
            "its mana cost."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Marie Magny"
        flavorText = "They called her unique insight a \"gift.\" Then she saw the truth, and suddenly it " +
            "was a \"threat.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/827ecc44-0b00-4515-8953-bc91fa03705a.jpg?1783916515"
        ruling(
            "2023-05-12",
            "Once a prowess ability triggers, causing the creature to lose prowess by removing Narset, " +
                "Enlightened Exile won't affect that ability. The creature will still get +1/+1 until end of turn."
        )
        ruling(
            "2023-05-12",
            "You cast the copy while the ability is resolving and still on the stack. You can't wait to cast " +
                "it later in the turn."
        )
        ruling("2023-05-12", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X.")
        ruling(
            "2023-05-12",
            "If another effect causes Narset's power to be less than or equal to the mana value of the target " +
                "card as the ability tries to resolve, the target is illegal. The card won't be exiled, and you " +
                "won't get to cast a copy."
        )
    }
}
