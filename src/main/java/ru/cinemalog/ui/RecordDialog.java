package ru.cinemalog.ui;

import ru.cinemalog.application.ValidationException;
import ru.cinemalog.domain.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;

public final class RecordDialog extends JDialog {
    public record Result(String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress) {}
    private final JTextField title = new JTextField(28); private final JComboBox<ContentType> type = new JComboBox<>(ContentType.values());
    private final JSpinner year = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 1888, 9999, 1));
    private final JComboBox<Genre> genre = new JComboBox<>(Genre.values()); private final JComboBox<Status> status = new JComboBox<>(Status.values()); private final JComboBox<Integer> rating = new JComboBox<>(); private final JTextArea comment = new JTextArea(5,28);
    private final JPanel seriesPanel = new JPanel(new BorderLayout(6, 6)); private final JTextField totalSeasons = new JTextField(5); private final JTextField watchedSeasons = new JTextField(5); private final JPanel seasonsRows = new JPanel(); private final JScrollPane seasonsScroll = new JScrollPane(seasonsRows);
    private final JLabel error = new JLabel(" "); private Result result; private final SeriesProgress initialProgress;
    public RecordDialog(Window owner, String titleText, MovieRecord initial) {
        super(owner,titleText,ModalityType.APPLICATION_MODAL); setDefaultCloseOperation(DISPOSE_ON_CLOSE); setSize(680,780); setLocationRelativeTo(owner);
        seasonsScroll.setPreferredSize(new Dimension(440, 220));
        this.initialProgress = initial == null ? null : initial.seriesProgress();
        for(int i=1;i<=10;i++)rating.addItem(i); rating.setSelectedItem(null); status.setSelectedItem(Status.WANT_TO_WATCH); build(initial);
    }
    private void build(MovieRecord initial){
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(new EmptyBorder(10,10,10,10));
        JPanel form=new JPanel(new GridBagLayout()); GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(4,4,4,4);g.anchor=GridBagConstraints.WEST;g.fill=GridBagConstraints.HORIZONTAL;
        int r=0; add(form,g,r++,"Название*",title);add(form,g,r++,"Тип контента*",type);add(form,g,r++,"Год выпуска",year);add(form,g,r++,"Жанр",genre);add(form,g,r++,"Статус*",status);add(form,g,r++,"Оценка",rating);add(form,g,r++,"Комментарий",new JScrollPane(comment));
        JPanel seriesHeader=new JPanel(new GridBagLayout());GridBagConstraints sg=new GridBagConstraints();sg.insets=new Insets(3,3,3,3);sg.anchor=GridBagConstraints.WEST;
        sg.gridx=0;sg.gridy=0;seriesHeader.add(new JLabel("Всего сезонов*"),sg);sg.gridx=1;seriesHeader.add(totalSeasons,sg);sg.gridx=2;seriesHeader.add(new JLabel("Просмотрено сезонов"),sg);sg.gridx=3;seriesHeader.add(watchedSeasons,sg);
        seriesPanel.setBorder(BorderFactory.createTitledBorder("Данные сериала")); seriesPanel.setVisible(false); seriesPanel.add(seriesHeader,BorderLayout.NORTH); seriesPanel.add(seasonsScroll,BorderLayout.CENTER);
        form.add(seriesPanel,grid(g,0,r++,2,1));
        type.addActionListener(e->rebuildSeries());
        totalSeasons.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){buildSeasonRows();}public void removeUpdate(DocumentEvent e){buildSeasonRows();}public void changedUpdate(DocumentEvent e){buildSeasonRows();}});
        watchedSeasons.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){applyWatchedSeasons();}public void removeUpdate(DocumentEvent e){applyWatchedSeasons();}public void changedUpdate(DocumentEvent e){applyWatchedSeasons();}});
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton cancel=new JButton("Отмена"), save=new JButton("Сохранить");buttons.add(cancel);buttons.add(save);cancel.addActionListener(e->{result=null;dispose();});save.addActionListener(e->submit());
        error.setForeground(new Color(170,0,0)); root.add(form,BorderLayout.CENTER);root.add(error,BorderLayout.SOUTH);root.add(buttons,BorderLayout.PAGE_END);setContentPane(root);
        if(initial!=null) fill(initial); rebuildSeries();
    }
    private GridBagConstraints grid(GridBagConstraints src,int x,int y,int w,int h){GridBagConstraints g=(GridBagConstraints)src.clone();g.gridx=x;g.gridy=y;g.gridwidth=w;g.gridheight=h;return g;}
    private void add(JPanel p,GridBagConstraints base,int row,String label,Component c){p.add(new JLabel(label),grid(base,0,row,1,1));p.add(c,grid(base,1,row,1,1));}
    private void fill(MovieRecord r){title.setText(r.title());type.setSelectedItem(r.contentType());if(r.releaseYear()!=null)year.setValue(r.releaseYear());genre.setSelectedItem(r.genre());status.setSelectedItem(r.status());rating.setSelectedItem(r.rating());comment.setText(r.comment());if(r.seriesProgress()!=null){totalSeasons.setText(String.valueOf(r.seriesProgress().totalSeasons()));watchedSeasons.setText(String.valueOf(r.seriesProgress().watchedSeasons()));}}
    private void rebuildSeries(){seriesPanel.removeAll();if(type.getSelectedItem()!=ContentType.SERIES){seriesPanel.setVisible(false);pack();return;}JPanel seriesHeader=new JPanel(new GridBagLayout());GridBagConstraints sg=new GridBagConstraints();sg.insets=new Insets(3,3,3,3);sg.anchor=GridBagConstraints.WEST;sg.gridx=0;sg.gridy=0;seriesHeader.add(new JLabel("Всего сезонов*"),sg);sg.gridx=1;seriesHeader.add(totalSeasons,sg);sg.gridx=2;seriesHeader.add(new JLabel("Просмотрено сезонов"),sg);sg.gridx=3;seriesHeader.add(watchedSeasons,sg);seriesPanel.setVisible(true);seriesPanel.setBorder(BorderFactory.createTitledBorder("Данные сериала"));seriesPanel.add(seriesHeader,BorderLayout.NORTH);seriesPanel.add(seasonsScroll,BorderLayout.CENTER);buildSeasonRows();pack();}
    private void buildSeasonRows(){if(initialProgress==null&&watchedSeasons.getText().isBlank())watchedSeasons.setText("0");seasonsRows.setLayout(new BoxLayout(seasonsRows,BoxLayout.Y_AXIS));seasonsRows.removeAll();try{int n=Integer.parseInt(totalSeasons.getText().trim());if(n<1||n>99)return;for(int i=1;i<=n;i++){JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));row.add(new JLabel("Сезон "+i+":"));row.add(new JLabel("серий"));JTextField ep=new JTextField(4);ep.setText(episodes(i-1));row.add(ep);row.add(new JLabel("просмотрено"));JTextField wa=new JTextField(4);wa.setText(watched(i-1));row.add(wa);seasonsRows.add(row);}seasonsRows.revalidate();seasonsRows.repaint();applyWatchedSeasons();}catch(NumberFormatException ignored){}}
    private void applyWatchedSeasons(){try{int ws=watchedSeasons.getText().isBlank()?0:Integer.parseInt(watchedSeasons.getText().trim());Component[] comps=seasonsRows.getComponents();for(int i=0;i<comps.length;i++){JPanel row=(JPanel)comps[i];JTextField t=(JTextField)row.getComponent(2);JTextField w=(JTextField)row.getComponent(4);if(i<ws){String s=w.getText().trim();if(s.isEmpty()||"0".equals(s))w.setText(t.getText());}}seasonsRows.revalidate();seasonsRows.repaint();}catch(NumberFormatException ignored){}}
    private String episodes(int idx){return initialProgress!=null&&idx<initialProgress.episodesPerSeason().size()?String.valueOf(initialProgress.episodesPerSeason().get(idx)):"1";}
    private String watched(int idx){return initialProgress!=null&&idx<initialProgress.watchedEpisodesPerSeason().size()?String.valueOf(initialProgress.watchedEpisodesPerSeason().get(idx)):"0";}
    private void submit(){try{Integer y=(Integer)year.getValue();Integer rt=(Integer)rating.getSelectedItem();SeriesProgress sp=null;if(type.getSelectedItem()==ContentType.SERIES){String ts=totalSeasons.getText().trim();if(ts.isEmpty()){error.setText("Сезонов не может быть более 99 и менее 1");return;}int n=Integer.parseInt(ts);int ws=watchedSeasons.getText().isBlank()?0:Integer.parseInt(watchedSeasons.getText());java.util.List<Integer> totals=new ArrayList<>(),watched=new ArrayList<>();for(Component c:seasonsRows.getComponents()){JPanel row=(JPanel)c;JTextField t=(JTextField)row.getComponent(2),w=(JTextField)row.getComponent(4);totals.add(Integer.parseInt(t.getText()));watched.add(Integer.parseInt(w.getText()));}sp=new SeriesProgress(n,totals,ws,watched);}result=new Result(title.getText(),(ContentType)type.getSelectedItem(),y,(Genre)genre.getSelectedItem(),(Status)status.getSelectedItem(),rt,comment.getText(),sp);dispose();}catch(Exception ex){error.setText(ex.getMessage()==null?"Проверьте введенные данные":ex.getMessage());}}
    public Result result(){return result;}
    public void showValidationErrors(ValidationException ex){error.setText(ex.errors().values().stream().findFirst().orElse("Проверьте данные"));}
}