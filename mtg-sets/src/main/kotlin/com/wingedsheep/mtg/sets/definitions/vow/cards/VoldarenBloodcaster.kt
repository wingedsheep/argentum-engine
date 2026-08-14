package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

private val bloodFilter = GameObjectFilter.Artifact.withSubtype("Blood")

private val VoldarenBloodcasterFront = card("Voldaren Bloodcaster") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Wizard"
    oracleText = "Flying\n" +
        "Whenever this creature or another nontoken creature you control dies, create a Blood token. " +
        "(It's an artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you create a Blood token, if you control five or more Blood tokens, transform this creature."
    power = 2
    toughness = 1
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().nontoken(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.CreateBlood()
    }

    // A Blood token entering under your control is necessarily one you just created: tokens cannot
    // move onto the battlefield from another zone. This therefore matches the printed trigger.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = bloodFilter.youControl().token(),
            binding = TriggerBinding.ANY,
        )
        triggerCondition = Conditions.YouControlAtLeast(5, bloodFilter)
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Kim Sokol"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca5297a5-bcaa-41fd-a397-e44dc4e00ba3.jpg?1783924857"
        ruling("2025-01-24", "Voldaren Bloodcaster's last ability checks how many Blood tokens you have both as it triggers and as it resolves. If you don't control at least five Blood tokens at both of those times, it won't transform.")
    }
}

private val BloodbatSummoner = card("Bloodbat Summoner") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Vampire Wizard"
    oracleText = "Flying\nAt the beginning of combat on your turn, up to one target Blood token " +
        "you control becomes a 2/2 black Bat creature with flying and haste in addition to its other types."
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val blood = target(
            "up to one target Blood token you control",
            TargetPermanent(
                optional = true,
                filter = TargetFilter(bloodFilter.youControl()),
            ),
        )
        effect = Effects.BecomeCreature(
            target = blood,
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            creatureTypes = setOf("Bat"),
            addTypes = setOf(CardType.ARTIFACT.name),
            colors = setOf(Color.BLACK.name),
            duration = Duration.Permanent,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Kim Sokol"
        flavorText = "\"Humans throw rice and release doves at their weddings? How quaint.\""
        imageUri = "https://cards.scryfall.io/normal/back/c/a/ca5297a5-bcaa-41fd-a397-e44dc4e00ba3.jpg?1783924857"
        ruling("2025-01-24", "Blood tokens that become Bats with Bloodbat Summoner's ability are still Blood artifact tokens and still have their activated ability.")
    }
}

val VoldarenBloodcaster: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = VoldarenBloodcasterFront,
    backFace = BloodbatSummoner,
)
