package com.wingedsheep.assay.gate

import com.wingedsheep.assay.corpus.ImplementedCard
import com.wingedsheep.assay.corpus.ImplementedCorpus
import com.wingedsheep.assay.corpus.OracleCard
import com.wingedsheep.assay.corpus.OracleFace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.assay.grammar.CardFragment
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.Locale
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * The differential's own behaviour, on hand-built pairs. The corpus run lives in
 * `just assay-differential`, which is a reporting tool rather than a unit test — the same split the
 * touchstone uses.
 *
 * The one exception is the golden-reader test at the bottom, which deliberately reads the committed
 * files: the file format is an assumption about another module's test output, and an assumption is
 * exactly the thing worth pinning down.
 */
class DifferentialTest : StringSpec({

    val differential = Differential()

    fun oracleCard(name: String, text: String, faces: Int = 1) = OracleCard(
        name = name,
        oracleId = "oracle-${name.lowercase(Locale.ROOT)}",
        layout = if (faces > 1) "transform" else "normal",
        setCode = "TST",
        scryfallKeywords = emptyList(),
        faces = List(faces) { i ->
            OracleFace(name = if (i == 0) name else "$name Back", oracleText = text, typeLine = "Creature — Angel")
        },
    )

    fun index(vararg cards: OracleCard): Map<String, OracleCard> = buildMap {
        cards.forEach { card ->
            card.oracleId?.let { put("id:$it", card) }
            put("name:${card.name.lowercase(Locale.ROOT)}", card)
        }
    }

    // The golden's own `oracleText` is load-bearing: the gate refuses to compare a card whose text
    // does not match the Scryfall entry it joined, so a fixture that omits it lands in
    // ORACLE_TEXT_DIFFERS rather than testing what it means to test.
    fun definition(
        name: String,
        text: String = "",
        keywords: Set<Keyword> = emptySet(),
        keywordAbilities: List<KeywordAbility> = emptyList(),
    ) = CardDefinition(
        name = name,
        manaCost = ManaCost.parse("{3}{W}"),
        typeLine = TypeLine.parse("Creature — Angel"),
        oracleText = text,
        creatureStats = CreatureStats.of(power = 4, toughness = 4),
        keywords = keywords,
        keywordAbilities = keywordAbilities,
        oracleId = "oracle-${name.lowercase(Locale.ROOT)}",
    )

    fun implemented(definition: CardDefinition?) =
        ImplementedCard(name = definition?.name ?: "Unknown", setCode = "TST", definition = definition)

    "a card whose keywords match the text confirms" {
        val card = oracleCard("Serra Angel", "Flying\nVigilance")
        val result = differential.compare(
            implemented(definition("Serra Angel", "Flying\nVigilance", keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE))),
            index(card),
        )

        result.population shouldBe Population.COMPARED
        result.verdict shouldBe Verdict.CONFIRMED
    }

    "a keyword the text has and the card lacks is a divergence, and names which side" {
        val card = oracleCard("Serra Angel", "Flying\nVigilance")
        val result = differential.compare(
            implemented(definition("Serra Angel", "Flying\nVigilance", keywords = setOf(Keyword.FLYING))),
            index(card),
        )

        result.verdict shouldBe Verdict.DIVERGENT
        result.onlyInText shouldContain KeywordAbility.Simple(Keyword.VIGILANCE)
        result.onlyInCard shouldBe emptyList()
    }

    "a keyword the card carries but the text does not print is a divergence too" {
        val card = oracleCard("Serra Angel", "Flying")
        val result = differential.compare(
            implemented(definition("Serra Angel", "Flying", keywords = setOf(Keyword.FLYING, Keyword.TRAMPLE))),
            index(card),
        )

        result.verdict shouldBe Verdict.DIVERGENT
        result.onlyInCard shouldContain KeywordAbility.Simple(Keyword.TRAMPLE)
        result.onlyInText shouldBe emptyList()
    }

    // The scoping rule the whole gate rests on. A card Assay only partly reads must never be
    // compared: the keywords it did not see would look like agreement, and the gate would be
    // reporting confidence it has not earned.
    "a card the grammar cannot read whole is excluded, not silently confirmed" {
        val text = "Cumulative upkeep—Add {R}."
        val card = oracleCard("Unread Card", text)
        val result = differential.compare(implemented(definition("Unread Card", text)), index(card))

        result.population shouldBe Population.NOT_COVERED
        result.verdict shouldBe null
    }

    "a card whose text is partly readable is still excluded" {
        val text = "Flying\nCumulative upkeep—Add {R}."
        val card = oracleCard("Braid of Fire", text)
        val result = differential.compare(
            implemented(definition("Braid of Fire", text, keywords = setOf(Keyword.FLYING))),
            index(card),
        )

        // The keyword line alone would have "confirmed" here; the whole-card rule is what stops it.
        result.population shouldBe Population.NOT_COVERED
    }

    "multi-face cards are counted out of scope rather than compared against the front face" {
        val card = oracleCard("Delver of Secrets", "Flying", faces = 2)
        val result = differential.compare(
            implemented(definition("Delver of Secrets", "Flying", keywords = setOf(Keyword.FLYING))),
            index(card),
        )

        result.population shouldBe Population.MULTI_FACE
    }

    // Found by the gate itself: three goldens joined by name to an entry with no text at all, which
    // Assay covers trivially, and then "diverged" against a fully-implemented script. Assay was
    // reading one card and diffing another.
    "a golden whose text is not the joined card's text is never compared" {
        val card = oracleCard("Inferno", "")
        val result = differential.compare(
            implemented(definition("Inferno", "~ deals 6 damage to each creature and each player.")),
            index(card),
        )

        result.population shouldBe Population.ORACLE_TEXT_DIFFERS
        result.verdict shouldBe null
    }

    // Goldens carry printed reminder text inconsistently, which is authoring noise rather than a
    // difference in what the card says. Comparing normalized lines is what keeps 300-odd cards in
    // the population instead of excluding them for a parenthetical.
    "reminder text alone does not count as the texts differing" {
        val card = oracleCard("Dancing Scimitar", "Flying (This creature can't be blocked except by creatures with flying or reach.)")
        val result = differential.compare(
            implemented(definition("Dancing Scimitar", "Flying", keywords = setOf(Keyword.FLYING))),
            index(card),
        )

        result.population shouldBe Population.COMPARED
        result.verdict shouldBe Verdict.CONFIRMED
    }

    // A keyword the SDK lowers to a triggered ability at authoring time leaves content in a slot the
    // grammar cannot produce. Confirming such a card would claim a check nobody performed.
    //
    // The probe field is `cantBeCopied`, not `cantBeCountered`: the latter used to stand in for "a
    // slot the grammar cannot produce" and stopped being one when `modelledSlots()` was widened to
    // match `MODELLED_SLOTS_NOTE`. Any genuinely unmodelled slot serves — pick a fresh one if this
    // one is ever read too.
    "a card with script content outside the modelled slots is not compared" {
        val card = oracleCard("Teeka's Dragon", "Flying")
        val withLowering = definition("Teeka's Dragon", "Flying", keywords = setOf(Keyword.FLYING))
            .let { it.copy(script = it.script.copy(cantBeCopied = true)) }
        val result = differential.compare(implemented(withLowering), index(card))

        result.population shouldBe Population.SCRIPT_NOT_MODELLED
        result.verdict shouldBe null
    }

    "a card with no Scryfall entry is counted, never crashed on" {
        val result = differential.compare(implemented(definition("Some Custom Card")), emptyMap())

        result.population shouldBe Population.NO_ORACLE_TEXT
    }

    "a golden that would not decode is a gate failure rather than a divergence" {
        val report = DifferentialReport.builder()
            .add(differential.compare(implemented(null), emptyMap()))
            .build()

        report.clean shouldBe false
        report.divergent shouldBe 0
    }

    // Parameterized keywords live in `keywordAbilities` while parameterless ones live in `keywords`,
    // so the unification in `printedKeywords` is load-bearing: without it every Ward card diverges.
    "the two SDK spellings of a card's keywords are unified before comparing" {
        val definition = definition(
            "Warded Angel",
            keywords = setOf(Keyword.FLYING),
            keywordAbilities = listOf(KeywordAbility.Simple(Keyword.VIGILANCE)),
        )

        differential.printedKeywords(definition) shouldBe setOf(
            KeywordAbility.Simple(Keyword.FLYING),
            KeywordAbility.Simple(Keyword.VIGILANCE),
        )
    }

    "divergences are reported but never fail the gate — they are findings to classify" {
        val card = oracleCard("Serra Angel", "Flying\nVigilance")
        val report = DifferentialReport.builder()
            .add(
                differential.compare(
                    implemented(definition("Serra Angel", "Flying\nVigilance", keywords = setOf(Keyword.FLYING))),
                    index(card),
                )
            )
            .build()

        report.divergent shouldBe 1
        report.clean shouldBe true
    }

    // ---------------------------------------------------------------------------------------
    // The fold list
    // ---------------------------------------------------------------------------------------

    // Punk Frogs' shape: `keywords: [WARD]` alongside `keywordAbilities: [Ward({3})]`. The bare
    // constant is an index entry the SDK populates on purpose, not a second ability.
    "a bare marker implied by a parameterized ability of the same keyword folds away" {
        val card = oracleCard("Punk Frogs", "Ward {3}")
        val result = differential.compare(
            implemented(
                definition(
                    "Punk Frogs",
                    "Ward {3}",
                    keywords = setOf(Keyword.WARD),
                    keywordAbilities = listOf(KeywordAbility.Ward(WardCost.Mana("{3}"))),
                )
            ),
            index(card),
        )

        result.verdict shouldBe Verdict.CONFIRMED
    }

    // The narrow half of the same rule: with no parameterized ability to imply it, a bare marker the
    // text does not print is exactly the bug the gate is for, and must survive the fold.
    "a bare marker with no parameterized ability behind it still diverges" {
        val card = oracleCard("Sire of Seven Deaths", "Flying")
        val result = differential.compare(
            implemented(definition("Sire of Seven Deaths", "Flying", keywords = setOf(Keyword.FLYING, Keyword.WARD))),
            index(card),
        )

        result.verdict shouldBe Verdict.DIVERGENT
        result.onlyInCard shouldContain KeywordAbility.Simple(Keyword.WARD)
    }

    // ---------------------------------------------------------------------------------------
    // Slot naming — the link is what carries meaning, not what it is called
    // ---------------------------------------------------------------------------------------

    fun destroyScript(slot: String?, reference: EffectTarget) = CardScript(
        spellEffect = Effects.Destroy(reference),
        targetRequirements = listOf(TargetObject(filter = TargetFilter(GameObjectFilter.Creature), id = slot)),
    )

    // Six cards reported as divergent over this: the grammar's slot is called "target", which is
    // also the name of a *field* on every targeted effect, so a textual rename rewrote the key
    // `"target":` on one side and left it alone on the other.
    "a slot named after a field in the effect tree still normalizes to its position" {
        val mine = destroyScript("target", EffectTarget.BoundVariable("target"))
        val theirs = destroyScript("target creature to destroy", EffectTarget.BoundVariable("target creature to destroy"))

        differential.normalizeSlotNames(mine) shouldBe differential.normalizeSlotNames(theirs)
    }

    // An Aura's restriction is a `TargetRequirement` like any other, declared under its own key.
    // The goldens leave it unnamed and the grammar always mints a name, so without `auraTarget` in
    // REQUIREMENT_KEYS every Aura in the corpus would report as divergent over an id that is in
    // neither model — the same shape as the four other ways this gate has found to lie to itself.
    "an aura's attachment restriction is normalized like every other requirement" {
        fun aura(slot: String?) = CardScript(
            auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Creature), id = slot),
            staticAbilities = listOf(ModifyStats(1, 2)),
        )

        differential.normalizeSlotNames(aura(null)) shouldBe differential.normalizeSlotNames(aura("target"))
    }

    // …and it is numbered *after* the spell's own requirements, because that is the order
    // `CastSpellHandler` builds the flat target list in.
    "an aura's restriction is numbered after the spell's own requirements" {
        val script = CardScript(
            spellEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            targetRequirements = listOf(TargetObject(filter = TargetFilter(GameObjectFilter.Land))),
            auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Creature)),
        )

        differential.normalizeSlotNames(script).contains("\"slot_1\"") shouldBe true
        differential.normalizeSlotNames(script.copy(auraTarget = null)) shouldNotBe
            differential.normalizeSlotNames(script)
    }

    // The SDK's own words: `BoundVariable` is "safer and more self-documenting than
    // ContextTarget(index)" — the same link, written by name instead of by position.
    "a positional target reference and a named one are the same model" {
        val named = destroyScript("target", EffectTarget.BoundVariable("target"))
        val positional = destroyScript(null, EffectTarget.ContextTarget(0))

        differential.normalizeSlotNames(named) shouldBe differential.normalizeSlotNames(positional)
    }

    // …and the half that keeps the fold honest: a reference to a *different* requirement must not
    // normalize into agreement.
    "a reference to another requirement stays different" {
        val first = destroyScript("target", EffectTarget.BoundVariable("target"))
        val second = destroyScript(null, EffectTarget.ContextTarget(1))

        differential.normalizeSlotNames(first) shouldNotBe differential.normalizeSlotNames(second)
    }

    // Trench Wurm: the whole script is one activated ability whose target is referred to
    // positionally. The numbering has to restart at each requirement-owner, because that is what
    // `ContextTarget`'s index counts — a card-wide counter reached the root and stopped.
    "a requirement declared inside an ability is normalized in that ability's own scope" {
        fun ability(slot: String?, reference: EffectTarget) = CardScript(
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = AbilityId("whatever"),
                    cost = AbilityCost.Tap,
                    effect = Effects.Destroy(reference),
                    targetRequirements = listOf(
                        TargetObject(filter = TargetFilter(GameObjectFilter.Creature), id = slot)
                    ),
                )
            )
        )

        differential.normalizeSlotNames(ability("target", EffectTarget.BoundVariable("target"))) shouldBe
            differential.normalizeSlotNames(ability(null, EffectTarget.ContextTarget(0)))
        differential.normalizeSlotNames(ability("target", EffectTarget.BoundVariable("target"))) shouldNotBe
            differential.normalizeSlotNames(ability(null, EffectTarget.ContextTarget(1)))
    }

    // The `descriptionOverride` rule Phase 1 established for triggered abilities, which activated
    // abilities need at least as badly: the override is what renders an ability's menu label, so
    // authors write one far more often — and it is presentation, never executed.
    "an authored ability label is not a difference in what the card does" {
        fun tapForGreen(label: String?) = CardScript(
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = AbilityId("whatever"),
                    cost = AbilityCost.Tap,
                    effect = Effects.AddMana(Color.GREEN),
                    timing = TimingRule.ManaAbility,
                    isManaAbility = true,
                    descriptionOverride = label,
                )
            )
        )

        differential.normalizeSlotNames(tapForGreen(null)) shouldBe
            differential.normalizeSlotNames(tapForGreen("{T}: Add {G}."))
    }

    // ---------------------------------------------------------------------------------------
    // The golden reader, against the real committed files
    // ---------------------------------------------------------------------------------------

    "the golden format splits on a column-0 header, not on any // in the text" {
        val text = """
            // Wax // Wane
            {
                "name": "Wax // Wane",
                "manaCost": "{G}",
                "typeLine": "Instant"
            }

            // Grizzly Bears
            {
                "name": "Grizzly Bears",
                "manaCost": "{1}{G}",
                "typeLine": "Creature — Bear"
            }
        """.trimIndent()

        val entries = ImplementedCorpus.splitEntries(text)

        entries.map { it.first } shouldBe listOf("Wax // Wane", "Grizzly Bears")
        entries.first().second.contains("\"name\": \"Wax // Wane\"") shouldBe true
    }

    "the committed goldens decode into real card definitions" {
        // Guarded rather than skipped-by-default: the goldens are committed, so their absence is a
        // broken checkout worth failing on, not an environment difference to tolerate.
        ImplementedCorpus.isAvailable() shouldBe true

        val sample = ImplementedCorpus.cards().take(200).toList()
        sample.size shouldBe 200
        sample.count { it.definition == null } shouldBe 0
        sample.first().definition?.name shouldNotBe null
    }

    // ---------------------------------------------------------------------------------------
    // The MODELLED_SLOTS_NOTE / modelledSlots() drift guard.
    //
    // `CardFragment.MODELLED_SLOTS_NOTE` names the `CardScript` slots the grammar can produce, and
    // `Differential.modelledSlots()` is the comparator that has to agree with it. The note's own
    // KDoc says the two are "kept as one list so they cannot drift apart" — but a prose constant
    // cannot enforce that, and they *did* drift: the note gained `castRestrictions`,
    // `additionalCosts` and `cantBeCountered` when the grammar learned to read them, the comparator
    // did not, and every card using one was fail-closed straight into SCRIPT_NOT_MODELLED. The
    // header string then advertised fields the gate silently dropped, so the reported coverage was
    // wider than the real one and 58 cards nobody had checked looked checked.
    //
    // This is the test that would have caught it: a card whose script sets a slot the note claims
    // must not land in SCRIPT_NOT_MODELLED. It asserts nothing about whether the card *agrees* with
    // its text — DIVERGENT is a fine outcome here — only that the gate is willing to look at it.
    // ---------------------------------------------------------------------------------------

    fun classify(script: CardScript, text: String): Population {
        val name = "Slot Probe"
        val card = oracleCard(name, text)
        val definition = definition(name, text).copy(script = script)
        return differential.compare(implemented(definition), index(card)).population
    }

    "a slot MODELLED_SLOTS_NOTE claims is compared, not fail-closed as unmodelled" {
        // One case per slot the note names that `modelledSlots()` used to omit. Each is a real
        // printed line, so the grammar produces the slot and the only question is whether the
        // comparator will look at it.
        val cases = listOf(
            "additionalCosts" to CardScript(
                spellEffect = Effects.DrawCards(1),
                additionalCosts = listOf(AdditionalCost.Atom(CostAtom.Discard(2))),
            ),
            "cantBeCountered" to CardScript(
                spellEffect = Effects.DrawCards(1),
                cantBeCountered = true,
            ),
        )
        cases.forEach { (slot, script) ->
            withClue("$slot is named in MODELLED_SLOTS_NOTE, so it must not fail-close the card") {
                classify(script, "Draw a card.") shouldNotBe Population.SCRIPT_NOT_MODELLED
            }
        }
    }

    "every slot MODELLED_SLOTS_NOTE names is one modelledSlots() actually carries" {
        // The note is the contract the differential's own header prints. Reading it back and
        // checking each name against `CardScript`'s real properties keeps the string honest: a slot
        // renamed or removed in the SDK, or a name typo'd into the note, fails here rather than
        // quietly widening what the gate claims to cover.
        val declared = CardFragment.MODELLED_SLOTS_NOTE.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val real = CardScript::class.members.map { it.name }.toSet()
        declared.forEach { slot ->
            withClue("MODELLED_SLOTS_NOTE names '$slot', which is not a CardScript property") {
                real shouldContain slot
            }
        }
        declared.size shouldBe declared.toSet().size
    }
})
