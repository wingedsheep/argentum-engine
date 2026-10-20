package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ayesha Tanaka — Legends #220
 * {W}{W}{U}{U} · Legendary Creature — Human Artificer · 2/2
 *
 * Banding
 * {T}: Counter target activated ability from an artifact source unless that ability's
 * controller pays {W}. (Mana abilities can't be targeted.)
 *
 * Brown Ouphe's target (activated ability whose source is an artifact) with Mirrorshell
 * Crab's counter-unless-pays on an ability; the payer is the ability's controller.
 */
val AyeshaTanaka = card("Ayesha Tanaka") {
    manaCost = "{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "Banding (Any creatures with banding, and up to one without, can attack in a band. " +
        "Bands are blocked as a group. If any creatures with banding you control are blocking or " +
        "being blocked by a creature, you divide that creature's combat damage, not its controller, " +
        "among any of the creatures it's being blocked by or is blocking.)\n" +
        "{T}: Counter target activated ability from an artifact source unless that ability's " +
        "controller pays {W}. (Mana abilities can't be targeted.)"

    keywords(Keyword.BANDING)

    activatedAbility {
        target(TargetFilter.ActivatedAbilityOnStack.abilitySourceMatches(GameObjectFilter.Artifact))
        cost = Costs.Tap
        effect = Effects.CounterUnlessPays("{W}", target = CounterTarget.Ability)
        description = "{T}: Counter target activated ability from an artifact source unless that " +
            "ability's controller pays {W}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03bd3d4b-d01a-475d-bf3b-cf96f43bc9ef.jpg?1783948041"
        ruling("6/8/2016", "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keywords are activated abilities and will have colons in their reminder text.")
        ruling("10/1/2009", "If a creature in combat has banding, its controller assigns damage for creatures blocking or blocked by it.")
    }
}
