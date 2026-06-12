package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trystan, Callous Cultivator // Trystan, Penitent Culler
 * {2}{G}
 * Legendary Creature — Elf Druid // Legendary Creature — Elf Warlock (transform)
 * 3/4 // 3/4
 *
 * Front — Trystan, Callous Cultivator
 *   Deathtouch
 *   Whenever this creature enters or transforms into Trystan, Callous Cultivator, mill three
 *   cards. Then if there is an Elf card in your graveyard, you gain 2 life.
 *   At the beginning of your first main phase, you may pay {B}. If you do, transform Trystan.
 *
 * Back — Trystan, Penitent Culler
 *   Deathtouch
 *   Whenever this creature transforms into Trystan, Penitent Culler, mill three cards, then you
 *   may exile an Elf card from your graveyard. If you do, each opponent loses 2 life.
 *   At the beginning of your first main phase, you may pay {G}. If you do, transform Trystan.
 */

private val millThenGainLifeIfElf = Effects.Composite(
    listOf(
        Patterns.Library.mill(3),
        ConditionalEffect(
            condition = Conditions.GraveyardContainsSubtype(Subtype.ELF),
            effect = Effects.GainLife(2)
        )
    )
)

private val millThenExileElfThenDrain = Effects.Pipeline {
    run(Patterns.Library.mill(3))
    val elfChoices = gather(
        CardSource.FromZone(
            zone = Zone.GRAVEYARD,
            player = Player.You,
            filter = GameObjectFilter.Any.withSubtype(Subtype.ELF)
        ),
        name = "elfChoices"
    )
    val exiledElf = chooseUpTo(
        1,
        from = elfChoices,
        chooser = Chooser.Controller,
        prompt = "Exile an Elf card from your graveyard (or cancel)",
        alwaysPrompt = true,
        name = "exiledElf"
    )
    move(exiledElf, destination = CardDestination.ToZone(Zone.EXILE))
    ifNotEmpty(exiledElf) {
        run(
            Effects.LoseLife(
                amount = 2,
                target = EffectTarget.PlayerRef(Player.EachOpponent)
            )
        )
    }
}

private val TrystanPenitentCuller = card("Trystan, Penitent Culler") {
    manaCost = ""
    typeLine = "Legendary Creature — Elf Warlock"
    power = 3
    toughness = 4
    oracleText = "Deathtouch\n" +
        "Whenever this creature transforms into Trystan, Penitent Culler, mill three cards, " +
        "then you may exile an Elf card from your graveyard. If you do, each opponent loses 2 life.\n" +
        "At the beginning of your first main phase, you may pay {G}. If you do, transform Trystan."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.TransformsToBack
        effect = millThenExileElfThenDrain
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{G}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Annie Stegg"
        imageUri = "https://cards.scryfall.io/normal/back/2/0/2094f9b6-ce84-47d9-8819-81db14ba483f.jpg?1767658448"
    }
}

private val TrystanCallousCultivatorFrontFace = card("Trystan, Callous Cultivator") {
    manaCost = "{2}{G}"
    typeLine = "Legendary Creature — Elf Druid"
    power = 3
    toughness = 4
    oracleText = "Deathtouch\n" +
        "Whenever this creature enters or transforms into Trystan, Callous Cultivator, mill " +
        "three cards. Then if there is an Elf card in your graveyard, you gain 2 life.\n" +
        "At the beginning of your first main phase, you may pay {B}. If you do, transform Trystan."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = millThenGainLifeIfElf
    }

    triggeredAbility {
        trigger = Triggers.TransformsToFront
        effect = millThenGainLifeIfElf
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Annie Stegg"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2094f9b6-ce84-47d9-8819-81db14ba483f.jpg?1767658448"
    }
}

val TrystanCallousCultivator: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TrystanCallousCultivatorFrontFace,
    backFace = TrystanPenitentCuller
)
